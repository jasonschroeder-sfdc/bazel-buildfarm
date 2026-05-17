# A minimal container for building a base worker image.
# Buildfarm public releases are build using this image as a starting point.
FROM ubuntu:22.04@sha256:4f838adc7181d9039ac795a7d0aba05a9bd9ecd480d294483169c5def983b64d

RUN apt-get update
RUN apt-get -y install default-jre default-jdk build-essential libfuse2 cgroup-tools
