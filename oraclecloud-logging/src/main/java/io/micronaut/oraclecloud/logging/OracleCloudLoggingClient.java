/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.oraclecloud.logging;

import com.oracle.bmc.loggingingestion.Logging;
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest;
import com.oracle.bmc.loggingingestion.responses.PutLogsResponse;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

/**
 * OracleCloudLoggingClient. TBA
 * <p> TBA
 *
 * @author Nemanja Mikic
 * @since 1.0.0
 */
@Singleton
@Context
public final class OracleCloudLoggingClient {

    private static Logging logging;
    private static String host;
    private static String appName;

    public OracleCloudLoggingClient(Logging logging, EmbeddedServer embeddedServer) {
        setLogging(logging, embeddedServer.getHost(), embeddedServer.getApplicationConfiguration().getName().orElse(""));
    }

    protected static synchronized Logging getLogging() {
        return logging;
    }

    protected static synchronized String getHost() {
        return host;
    }

    protected static synchronized String getAppName() {
        return appName;
    }

    protected static synchronized void setLogging(Logging logging, String host, String appName) {
        OracleCloudLoggingClient.logging = logging;
        OracleCloudLoggingClient.host = host;
        OracleCloudLoggingClient.appName = appName;
    }

    protected static synchronized void destroy() throws Exception {
        OracleCloudLoggingClient.logging.close();
        OracleCloudLoggingClient.logging = null;
    }

    @PreDestroy
    public void close() throws Exception {
        OracleCloudLoggingClient.destroy();
    }

    protected static synchronized boolean putLogs(PutLogsRequest putLogsRequest) {
        if (logging != null) {
            PutLogsResponse putLogsResponse = logging.putLogs(putLogsRequest);
            return putLogsResponse.getOpcRequestId() != null;
        }
        return false;
    }
}
