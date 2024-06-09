java_library(
    name = "observation",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:io_micrometer_micrometer_observation",
        "@maven//:io_micrometer_micrometer_tracing",
        "@maven//:io_micrometer_micrometer_tracing_bridge_otel",
        "@maven//:io_opentelemetry_opentelemetry_api",
        "@maven//:io_opentelemetry_opentelemetry_context",
        "@maven//:io_opentelemetry_opentelemetry_exporter_zipkin",
        "@maven//:io_opentelemetry_opentelemetry_extension_trace_propagators",
        "@maven//:io_opentelemetry_opentelemetry_sdk",
        "@maven//:io_opentelemetry_opentelemetry_sdk_extension_autoconfigure",
        "@maven//:io_opentelemetry_opentelemetry_sdk_trace",
    ],
)
