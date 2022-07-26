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

/**
 * StackdriverTraceConstants.
 * <p> TBA
 *
 * @author Nemanja Mikic
 * @since 1.0.0
 */
public interface OracleCloudTraceConstants {

    /**
     * The JSON field name for the log level (severity).
     */
    String SEVERITY_ATTRIBUTE = "severity";

    /**
     * The JSON field name for the seconds of the timestamp.
     */
    String TIMESTAMP_SECONDS_ATTRIBUTE = "timestampSeconds";

    /**
     * The JSON field name for the nanos of the timestamp.
     */
    String TIMESTAMP_NANOS_ATTRIBUTE = "timestampNanos";

    /**
     * The JSON field name for the trace-id.
     */
    String TRACE_ID_ATTRIBUTE = "logging.trace_id";

    /**
     * The JSON field name for the span-id.
     */
    String SPAN_ID_ATTRIBUTE = "logging.span_id";

    /**
     * The name of the OpenTelemetry trace id field.
     */
    String MDC_FIELD_TRACE_ID = "trace_id";

    /**
     * The name of the OpenTelemetry span id field.
     */
    String MDC_FIELD_SPAN_ID = "span_id";

}
