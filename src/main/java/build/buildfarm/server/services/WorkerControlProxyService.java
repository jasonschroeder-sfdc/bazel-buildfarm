// Copyright 2026 The Buildfarm Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buildfarm.server.services;

import build.buildfarm.instance.Instance;
import build.buildfarm.v1test.PrepareWorkerForGracefulShutDownRequest;
import build.buildfarm.v1test.PrepareWorkerForGracefulShutDownRequestResults;
import build.buildfarm.v1test.WorkerControlGrpc;
import build.buildfarm.v1test.WorkerPipelineChangeRequest;
import build.buildfarm.v1test.WorkerPipelineChangeResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.java.Log;

@Log
public class WorkerControlProxyService extends WorkerControlGrpc.WorkerControlImplBase {
  Instance instance;

  public WorkerControlProxyService(Instance instance) {
    this.instance = instance;
  }

  @Override
  public void pipelineChange(
      WorkerPipelineChangeRequest request,
      StreamObserver<WorkerPipelineChangeResponse> responseObserver) {
    if (request.getChangesCount() > 0) {
      try {
        responseObserver.onNext(
            instance.pipelineChange(request.getWorkerName(), request.getChangesList()).get());
        responseObserver.onCompleted();
      } catch (Exception e) {
        responseObserver.onError(e);
      }
    } else {
      responseObserver.onNext(WorkerPipelineChangeResponse.newBuilder().build());
      responseObserver.onCompleted();
    }
  }

  @Override
  public void prepareWorkerForGracefulShutdown(
      PrepareWorkerForGracefulShutDownRequest request,
      StreamObserver<PrepareWorkerForGracefulShutDownRequestResults> responseObserver) {
    instance.shutDownWorkerGracefully(request.getWorkerName());
    responseObserver.onNext(PrepareWorkerForGracefulShutDownRequestResults.newBuilder().build());
    responseObserver.onCompleted();
  }
}
