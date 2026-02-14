load("@rules_java//java:java_binary.bzl", "java_binary")
load("//:jvm_flags.bzl", "server_jvm_flags", "worker_jvm_flags")

package(
    default_visibility = ["//src:__subpackages__"],
)

LOGGING_RUNTIME_DEPS = [
    "@buildfarm_maven//:ch_qos_logback_logback_classic",
    "@buildfarm_maven//:ch_qos_logback_contrib_logback_json_classic",
    "@buildfarm_maven//:ch_qos_logback_contrib_logback_jackson",
    "@buildfarm_maven//:com_fasterxml_jackson_core_jackson_databind",
]

java_binary(
    name = "buildfarm-server",
    classpath_resources = [
        "//examples:example_configs",
        "//examples:example_properties",
    ],
    jvm_flags = server_jvm_flags(),
    main_class = "build.buildfarm.server.BuildFarmServer",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//src/main/java/build/buildfarm/server",
    ] + LOGGING_RUNTIME_DEPS,
)

java_binary(
    name = "buildfarm-shard-worker",
    classpath_resources = [
        "//examples:example_configs",
        "//examples:example_properties",
    ],
    jvm_flags = worker_jvm_flags(),
    main_class = "build.buildfarm.worker.shard.Worker",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//src/main/java/build/buildfarm/worker/shard",
    ] + LOGGING_RUNTIME_DEPS,
)
