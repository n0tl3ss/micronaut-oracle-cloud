/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oci.function.http.test;

import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.testing.FnEventBuilder;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.HttpServerException;
import io.micronaut.http.server.exceptions.ServerStartupException;
import io.micronaut.oci.function.http.HttpFunction;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Mock HTTP server implementation for writing tests that simulate a Project.fn gateway.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Singleton
@Secondary
@Internal
final class MockFnHttpServer implements EmbeddedServer {
    private final ApplicationContext applicationContext;
    private final ExecutorService executorService;
    private int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Server server;

    public MockFnHttpServer(
            @Named(TaskExecutors.IO) ExecutorService executorService,
            ApplicationContext applicationContext) {
        this.executorService = executorService;
        this.applicationContext = applicationContext;
        this.port = SocketUtils.findAvailableTcpPort();
    }

    @Override
    public EmbeddedServer start() {
        if (running.compareAndSet(false, true)) {
            int retryCount = 0;
            while (retryCount <= 3) {
                try {
                    this.server = new Server(port);
                    this.server.setHandler(new FnHandler());
                    this.server.start();
                    break;
                } catch (BindException e) {
                    this.port = SocketUtils.findAvailableTcpPort();
                    retryCount++;
                } catch (Exception e) {
                    throw new ServerStartupException(e.getMessage(), e);
                }
            }
            if (server == null) {
                throw new HttpServerException("No available ports");
            }
        }
        return this;
    }

    @Override
    public EmbeddedServer stop() {
        if (running.compareAndSet(true, false)) {
            try {
                server.stop();
            } catch (Exception e) {
                // ignore / unrecoverable
            }
        }
        return this;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return "localhost";
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public URL getURL() {
        String spec = getScheme() + "://" + getHost() + ":" + getPort();
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new HttpServerException("Invalid server URL " + spec);
        }
    }

    @Override
    public URI getURI() {
        try {
            return getURL().toURI();
        } catch (URISyntaxException e) {
            throw new HttpServerException("Invalid server URL " + getURL());
        }
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationContext.getBean(ApplicationConfiguration.class);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private class FnHandler extends AbstractHandler {

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            FnTestingRule fn = FnTestingRule.createDefault();
            fn.addSharedClassPrefix("org.slf4j.");
            fn.addSharedClassPrefix("com.sun.");
            String queryString = request.getQueryString();
            String requestURI = request.getRequestURI();
            if (StringUtils.isNotEmpty(requestURI)) {
                requestURI += "?" + queryString;
            }
            FnEventBuilder<FnTestingRule> eventBuilder = fn.givenEvent()
                    .withHeader("Fn-Http-Request-Url", requestURI)
                    .withHeader("Fn-Http-Method", request.getMethod());
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String s = headerNames.nextElement();
                Enumeration<String> headers = request.getHeaders(s);
                while (headers.hasMoreElements()) {
                    String v = headers.nextElement();
                    eventBuilder.withHeader("Fn-Http-H-" + s, v);
                }
            }
            HttpMethod httpMethod = HttpMethod.parse(request.getMethod());
            if (HttpMethod.permitsRequestBody(httpMethod)) {
                try (InputStream requestBody = request.getInputStream()) {
                    eventBuilder.withBody(requestBody);
                } catch (IOException e) {
                    // ignore
                }
            }

            eventBuilder.enqueue();
            fn.thenRun(HttpFunction.class, "handleRequest");
            FnResult outputEvent = fn.getOnlyResult();
            HttpStatus httpStatus = outputEvent.getHeaders().get("Fn-Http-Status").map(s ->
                    HttpStatus.valueOf(Integer.parseInt(s))).orElseGet(() ->
                    outputEvent.getStatus() == OutputEvent.Status.Success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR
            );
            byte[] bodyAsBytes = outputEvent.getBodyAsBytes();
            outputEvent.getHeaders().asMap().forEach((key, strings) -> {
                if (key.startsWith("Fn-Http-H-")) {
                    String httpKey = key.substring("Fn-Http-H-".length());
                    if (httpKey.length() > 0) {
                        for (String string : strings) {
                            response.addHeader(httpKey, string);
                        }
                    }
                }
            });
            response.setStatus(httpStatus.getCode());
            response.setContentLength(bodyAsBytes.length);
            if (bodyAsBytes.length > 0) {
                try (OutputStream responseBody = response.getOutputStream()) {
                    responseBody.write(bodyAsBytes);
                    responseBody.flush();
                }
            }
        }
    }
}
