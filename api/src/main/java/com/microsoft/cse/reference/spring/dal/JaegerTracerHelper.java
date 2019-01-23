package com.microsoft.cse.reference.spring.dal;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;


/**
 * Helper functions for tracing and logs
 */
public class JaegerTracerHelper {

    /**
     * Initializes a Jaeger tracer
     * @param service the name of the service
     * @return the Jaeger tracer
     */
    public static JaegerTracer initTracer(String service) {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv().withType("const").withParam(1);
        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true);
        Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
        return config.getTracer();
    }
}