load("@buildifier_prebuilt//:rules.bzl", "buildifier")
load("@doxygen//:doxygen.bzl", "doxygen")

package(default_visibility = ["//visibility:public"])

# Made available for formatting
buildifier(
    name = "buildifier",
)

cc_binary(
    name = "as-nobody",
    srcs = select({
        "//config:windows": ["as-nobody-windows.c"],
        "//conditions:default": ["as-nobody.c"],
    }),
    tags = ["container"],
)

exports_files([
    "delay.sh",
    "macos-wrapper.sh",
    "_site/assets/images/buildfarm-logo.png",
])

doxygen(
    name = "doxygen",  # Name of the rule, can be anything
    srcs =
        #        glob(
        #            include = [
        #                # List of sources to document.
        #                #"src/main/java/build/buildfarm/**/*.java",
        #                #"persistentworkers/**/*.java",
        #                "src/test/java/build/buildfarm/AllTests.java",
        #            ],
        #        ) +
        [
            "README.md",
            "_site/assets/images/buildfarm-logo.png",
            "//src/main/java/build/buildfarm/actioncache:doxygen_srcs",
            "//src/main/java/build/buildfarm/backplane:doxygen_srcs",
            "//src/main/java/build/buildfarm/cas:doxygen_srcs",
            "//src/main/java/build/buildfarm/common:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/blake3:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/config:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/grpc:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/redis:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/resources:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/s3:doxygen_srcs",
            "//src/main/java/build/buildfarm/common/services:doxygen_srcs",
            "//src/main/java/build/buildfarm/instance:doxygen_srcs",
            "//src/main/java/build/buildfarm/instance/server:doxygen_srcs",
            "//src/main/java/build/buildfarm/instance/shard:doxygen_srcs",
            "//src/main/java/build/buildfarm/instance/stub:doxygen_srcs",
            "//src/main/java/build/buildfarm/metrics:doxygen_srcs",
            "//src/main/java/build/buildfarm/metrics/log:doxygen_srcs",
            "//src/main/java/build/buildfarm/metrics/prometheus:doxygen_srcs",
            "//src/main/java/build/buildfarm/proxy/http:doxygen_srcs",
            "//src/main/java/build/buildfarm/server/services:doxygen_srcs",
            "//src/main/java/build/buildfarm/tools:doxygen_srcs",  # TODO Javadoc?
            "//src/main/java/build/buildfarm/worker:doxygen_srcs",
            "//src/main/java/build/buildfarm/worker/cgroup:doxygen_srcs",
            "//src/main/java/build/buildfarm/worker/persistent:doxygen_srcs",
            "//src/main/java/build/buildfarm/worker/resources:doxygen_srcs",
            "//src/main/java/build/buildfarm/worker/shard:doxygen_srcs",
            "//src/main/java/build/buildfarm/worker/util:doxygen_srcs",
        ],
    configurations = [
        # Customizable configurations
        "GENERATE_HTML = YES",  # that override the default ones
        "GENERATE_LATEX = NO",  # from the Doxyfile
        "USE_MDFILE_AS_MAINPAGE = README.md",
        "OPTIMIZE_OUTPUT_JAVA = YES",  # Set the OPTIMIZE_OUTPUT_JAVA tag to YES if your project consists of Java or Python sources only. Doxygen will then generate output that is more tailored for that language.
        "NUM_PROC_THREADS = 3",
        "EXTRACT_ALL = YES",
    ],
    extract_private = True,
    full_sidebar = True,
    generate_treeview = True,
    project_brief = "Bazel remote caching and execution service",  # Brief description of the project
    project_icon = "_site/assets/images/buildfarm-logo.png",
    project_logo = "_site/assets/images/buildfarm-logo.png",  # Kind of big
    project_name = "buildfarm",  # Name of the project
    tags = [
        "cpu:4",
        "manual",
    ],  # Tags to add to the target.
    # This way the target won't run unless explicitly called
)
