java_library(
    name = "prometheus",
    srcs = [
        "PrometheusPublisher.java",
    ],
    plugins = ["//src/main/java/build/buildfarm/common:lombok"],
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:io_micrometer_micrometer_observation",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:io_micrometer_micrometer_tracing",
        "@maven//:io_micrometer_micrometer_tracing_bridge_otel",
        "@maven//:io_prometheus_prometheus_metrics_exporter_httpserver",
        "@maven//:org_projectlombok_lombok",
    ],
)
