package com.microsoft.cse.reference.spring.dal.config;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import org.springframework.context.annotation.Bean;


/**
 * Helper functions for tracing
 */
@org.springframework.context.annotation.Configuration
public class TracerConfig {

    @Bean
    public static Tracer initTracer() {

        final SamplerConfiguration samplerConfig = new SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        final Configuration.SenderConfiguration senderConfig = Configuration.SenderConfiguration.fromEnv();

        final ReporterConfiguration reporterConfig = new ReporterConfiguration()
                .withLogSpans(true)
                .withFlushInterval(1000)
                .withMaxQueueSize(10000)
                .withSender(senderConfig);

        return new Configuration("springboot-api").withSampler(samplerConfig)
                .withReporter(reporterConfig).getTracer();

    }
}