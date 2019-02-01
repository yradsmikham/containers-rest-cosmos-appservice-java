package com.microsoft.cse.reference.spring.dal;

import io.opentracing.Tracer;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;

import io.jaegertracing.internal.samplers.ConstSampler;

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

    public static Tracer initEnvTracer()
    {
        Tracer tracer = Configuration.fromEnv("custom-endpoint-controller-svc").getTracer();
        //Tracer tracer = TracerResolver.resolveTracer();
        //return tracer;

        return new Configuration("play-java-starter-example")
        .withReporter(
            Configuration.ReporterConfiguration.fromEnv()
                .withLogSpans(true)
                .withFlushInterval(1000)
                .withMaxQueueSize(10000)
                .withSender(
                    Configuration.SenderConfiguration.fromEnv()
                        //.withEndpoint("http://jaeger-collector:14268/api/traces")
                        //.withAgentHost("jaeger-agent.jaeger.svc.cluster.local")
                        //.withAgentPort(6832)
                )
        )
        .withSampler(
            Configuration.SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1)
        )
        .getTracer();
        //.getTracerBuilder()
        //.build();
    }

    public static Tracer initTracerWithConfig()
    {
        System.setProperty(Configuration.JAEGER_ENDPOINT, "https://jaeger-collector:14268/api/traces");
        System.setProperty(Configuration.JAEGER_REPORTER_LOG_SPANS, "true");
        System.setProperty(Configuration.JAEGER_REPORTER_FLUSH_INTERVAL, "1");
        System.setProperty(Configuration.JAEGER_SAMPLER_TYPE, "const");
        System.setProperty(Configuration.JAEGER_SAMPLER_PARAM, "1");
        System.setProperty(Configuration.JAEGER_SERVICE_NAME, "jackson-svc");

        Tracer tracer = Configuration.fromEnv("custom-endpoint-controller-svc").getTracer();
        return tracer;
    }
}