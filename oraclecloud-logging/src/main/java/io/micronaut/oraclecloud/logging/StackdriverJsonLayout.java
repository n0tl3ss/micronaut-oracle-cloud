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
package io.micronaut.oraclecloud.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.micronaut.oraclecloud.logging.StackdriverTraceConstants.MDC_FIELD_SPAN_ID;
import static io.micronaut.oraclecloud.logging.StackdriverTraceConstants.MDC_FIELD_TRACE_ID;

/**
 * StackdriverJsonLayout.
 * <p> TBA
 *
 * @author Nemanja Mikic
 * @since 1.0.0
 */
public class StackdriverJsonLayout extends JsonLayout {

    private boolean includeTraceId;

    private boolean includeSpanId;

    private boolean includeExceptionInMessage;

    private Map<String, Object> customJson;

    public StackdriverJsonLayout() {
        this.appendLineSeparator = true;
        this.includeExceptionInMessage = true;
        this.includeException = false;
        this.includeTraceId = true;
        this.includeSpanId = true;
        ObjectMapper mapper = new ObjectMapper();
        setJsonFormatter(mapper::writeValueAsString);
    }

    @Override
    public void start() {
        super.start();
    }

    /**
     * Convert a logging event into a Map.
     *
     * @param event the logging event
     * @return the map which should get rendered as JSON
     */
    @Override
    protected Map<String, Object> toJsonMap(ILoggingEvent event) {

        Map<String, Object> map = new LinkedHashMap<>();

        if (this.includeTimestamp) {
            map.put(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE,
                    TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
            map.put(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE,
                    TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp()));
        }

        add(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, this.includeLevel,
                String.valueOf(event.getLevel()), map);
        add(JsonLayout.THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
        add(JsonLayout.LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);

        if (this.includeFormattedMessage) {
            String message = event.getFormattedMessage();
            if (this.includeExceptionInMessage) {
                IThrowableProxy throwableProxy = event.getThrowableProxy();
                if (throwableProxy != null) {
                    String stackTrace = getThrowableProxyConverter().convert(event);
                    if (stackTrace != null && !stackTrace.equals("")) {
                        message += "\n" + stackTrace;
                    }
                }
            }
            map.put(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, message);
        }
        add(JsonLayout.MESSAGE_ATTR_NAME, this.includeMessage, event.getMessage(), map);
        add(JsonLayout.CONTEXT_ATTR_NAME, this.includeContextName, event.getLoggerContextVO().getName(), map);
        addThrowableInfo(JsonLayout.EXCEPTION_ATTR_NAME, this.includeException, event, map);
        add(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE, this.includeTraceId,
                event.getLoggerContextVO().getPropertyMap().get(MDC_FIELD_TRACE_ID), map);
        add(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, this.includeSpanId,
                event.getLoggerContextVO().getPropertyMap().get(MDC_FIELD_SPAN_ID), map);
        if (this.customJson != null && !this.customJson.isEmpty()) {
            for (Map.Entry<String, Object> entry : this.customJson.entrySet()) {
                map.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        addCustomDataToJsonMap(map, event);
        return map;
    }

    /**
     * @return should trace id be included
     */
    public boolean isIncludeTraceId() {
        return includeTraceId;
    }

    /**
     * @param includeTraceId sets traceId inclusion
     */
    public void setIncludeTraceId(boolean includeTraceId) {
        this.includeTraceId = includeTraceId;
    }

    /**
     * @return SpanId is included
     */
    public boolean isIncludeSpanId() {
        return includeSpanId;
    }

    /**
     * @param includeSpanId sets include span id
     */
    public void setIncludeSpanId(boolean includeSpanId) {
        this.includeSpanId = includeSpanId;
    }

    /**
     * @return include message in exception
     */
    public boolean isIncludeExceptionInMessage() {
        return includeExceptionInMessage;
    }

    /**
     * @param includeExceptionInMessage includeExceptionInMessage
     */
    public void setIncludeExceptionInMessage(boolean includeExceptionInMessage) {
        this.includeExceptionInMessage = includeExceptionInMessage;
    }

    /**
     * @return customJson
     */
    public Map<String, Object> getCustomJson() {
        return customJson;
    }

    /**
     * @param customJson sets CustomJson Map
     */
    public void setCustomJson(Map<String, Object> customJson) {
        this.customJson = customJson;
    }
}
