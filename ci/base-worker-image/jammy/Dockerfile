# A minimal container for building a base worker image.
# Buildfarm public releases are build using this image as a starting point.
FROM ubuntu:22.04@sha256:104ae83764a5119017b8e8d6218fa0832b09df65aae7d5a6de29a85d813da2fb

RUN apt-get update
RUN apt-get -y install default-jre default-jdk build-essential libfuse2 cgroup-tools
