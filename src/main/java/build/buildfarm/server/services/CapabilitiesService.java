// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buildfarm.server.services;

import build.bazel.remote.execution.v2.CapabilitiesGrpc;
import build.bazel.remote.execution.v2.GetCapabilitiesRequest;
import build.bazel.remote.execution.v2.ServerCapabilities;
import build.bazel.semver.SemVer;
import build.buildfarm.instance.Instance;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class CapabilitiesService extends CapabilitiesGrpc.CapabilitiesImplBase {
  // Prometheus metrics
  private final Counter numberOfRemoteInvocations =
      Counter.builder("remote.invocations")
          .description("Number of remote invocations.")
          .register(Metrics.globalRegistry);

  private final Instance instance;

  public CapabilitiesService(Instance instance) {
    this.instance = instance;
  }

  @Override
  public void getCapabilities(
      GetCapabilitiesRequest request, StreamObserver<ServerCapabilities> responseObserver) {
    numberOfRemoteInvocations.increment();
    responseObserver.onNext(
        instance.getCapabilities().toBuilder()
            .setLowApiVersion(SemVer.newBuilder().setMajor(2))
            .setHighApiVersion(SemVer.newBuilder().setMajor(2))
            .build());
    responseObserver.onCompleted();
  }
}
