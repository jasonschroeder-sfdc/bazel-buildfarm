# A minimal container for building a base worker image.
# Buildfarm public releases are build using this image as a starting point.
FROM ubuntu:22.04@sha256:5ec03bb3441e8b0bf3b4f9cd4629a1ae763010dc3035bb8da3ae6cf026486401

RUN apt-get update
RUN apt-get -y install default-jre default-jdk build-essential libfuse2 cgroup-tools
