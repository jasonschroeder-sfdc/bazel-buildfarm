load("@rules_java//java:java_test.bzl", "java_test")

java_test(
    name = "tests",
    srcs = glob(["*.java"]),
    test_class = "build.buildfarm.AllTests",
    deps = [
        "//src/main/java/build/buildfarm/common",
        "//src/main/java/build/buildfarm/common/base",
        "//src/main/java/build/buildfarm/common/grpc",
        "//src/test/java/build/buildfarm:test_runner",
        "@googleapis//google/bytestream:bytestream_java_grpc",
        "@googleapis//google/bytestream:bytestream_java_proto",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_jimfs_jimfs",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:commons_io_commons_io",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_grpc_grpc_core",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_testing",
        "@maven//:org_apache_commons_commons_compress",
        "@maven//:org_mockito_mockito_core",
    ],
)
