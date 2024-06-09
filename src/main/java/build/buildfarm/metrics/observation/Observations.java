package build.buildfarm.metrics.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.Collections;

public class Observations {

  private static final Observations INSTANCE = new Observations();
  private ObservationRegistry observationRegistry;

  private Observations() {

    // [OTel component] The SDK implementation of OpenTelemetry
    OpenTelemetrySdk openTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    // [OTel component] Tracer is a component that handles the life-cycle of a span
    io.opentelemetry.api.trace.Tracer otelTracer =
        openTelemetrySdk.getTracerProvider().get("io.micrometer.micrometer-tracing");
    final OtelPropagator propagator =
        new OtelPropagator(
            ContextPropagators.create(
                TextMapPropagator.composite(W3CTraceContextPropagator.getInstance())),
            otelTracer);

    // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel
    OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();

    // [Micrometer Tracing component] A Micrometer Tracing listener for setting up MDC
    Slf4JEventListener slf4JEventListener = new Slf4JEventListener();

    // [Micrometer Tracing component] A Micrometer Tracing listener for setting
    // Baggage in MDC. Customizable
    // with correlation fields (currently we're setting empty list)
    Slf4JBaggageEventListener slf4JBaggageEventListener =
        new Slf4JBaggageEventListener(Collections.emptyList());

    // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel's Tracer.
    // You can consider
    // customizing the baggage manager with correlation and remote fields (currently
    // we're setting empty lists)
    OtelTracer tracer =
        new OtelTracer(
            otelTracer,
            otelCurrentTraceContext,
            event -> {
              slf4JEventListener.onEvent(event);
              slf4JBaggageEventListener.onEvent(event);
            },
            new OtelBaggageManager(
                otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
    MeterRegistry meterRegistry = Metrics.globalRegistry;

    ObservationRegistry obsRegistry = ObservationRegistry.create();
    obsRegistry
        .observationConfig()
        // assuming that micrometer-core is on the classpath
        // TODO turn this on if you want Metrics from your Observations.
        // They would be in addition to the Prometheus metrics.
        // .observationHandler(new DefaultMeterObservationHandler(meterRegistry))
        // we set up a first matching handler that creates spans - it comes from
        // Micrometer
        // Tracing. We set up spans for sending and receiving data over the wire
        // and a default one
        .observationHandler(
            new ObservationHandler.FirstMatchingCompositeObservationHandler(
                new PropagatingSenderTracingObservationHandler<>(tracer, propagator),
                new PropagatingReceiverTracingObservationHandler<>(tracer, propagator),
                new DefaultTracingObservationHandler(tracer)));
  }

  public static ObservationRegistry getGlobalRegistry() {
    return INSTANCE.observationRegistry;
  }
}
