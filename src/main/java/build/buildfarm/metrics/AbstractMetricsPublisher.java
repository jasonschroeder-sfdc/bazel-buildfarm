// Copyright 2020 The Bazel Authors. All rights reserved.
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

package build.buildfarm.metrics;

import build.bazel.remote.execution.v2.ExecuteOperationMetadata;
import build.bazel.remote.execution.v2.ExecuteResponse;
import build.bazel.remote.execution.v2.ExecutedActionMetadata;
import build.bazel.remote.execution.v2.RequestMetadata;
import build.buildfarm.common.Time;
import build.buildfarm.v1test.OperationRequestMetadata;
import com.google.common.annotations.VisibleForTesting;
import com.google.longrunning.Operation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.PreconditionFailure;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.extern.java.Log;

@Log
public abstract class AbstractMetricsPublisher implements MetricsPublisher {
  private final Counter actionsCounter =
      Counter.builder("actions").description("Number of actions.").register(Metrics.globalRegistry);

  private static final Timer queuedTime =
      Timer.builder("queued.time").description("Queued time").register(Metrics.globalRegistry);
  private static final Timer outputUploadTime =
      Timer.builder("output.upload")
          .description("Output upload time")
          .register(Metrics.globalRegistry);

  private final String clusterId;

  public AbstractMetricsPublisher(String clusterId) {
    this.clusterId = clusterId;
  }

  public AbstractMetricsPublisher() {
    this(/* clusterId= */ null);
  }

  private static Counter operationsPerWorkerCounter(String operationWorker) {
    return Counter.builder("operation.worker")
        .tag("worker", operationWorker)
        .description("Operations per worker.")
        .register(Metrics.globalRegistry);
  }

  private static Counter operationExitCodeCounter(String exitCode) {
    return Counter.builder("operation.exit.code")
        .tag("exit_code", exitCode)
        .description("Operation execution exit code.")
        .register(Metrics.globalRegistry);
  }

  private static Counter operationsInStageCounter(String stageName) {
    return Counter.builder("operations.stage.load")
        .tag("stage_name", stageName)
        .description("Operations in stage.")
        .register(Metrics.globalRegistry);
  }

  private static Counter operationStatusCounter(String code) {
    return Counter.builder("operation.status")
        .tag("code", code)
        .description("Operation execution status.")
        .register(Metrics.globalRegistry);
  }

  @Override
  public void publishRequestMetadata(Operation operation, RequestMetadata requestMetadata) {
    throw new UnsupportedOperationException("Not Implemented.");
  }

  @Override
  public abstract void publishMetric(String metricName, Object metricValue);

  @VisibleForTesting
  protected OperationRequestMetadata populateRequestMetadata(
      Operation operation, RequestMetadata requestMetadata) {
    try {
      actionsCounter.increment();
      OperationRequestMetadata operationRequestMetadata =
          OperationRequestMetadata.newBuilder()
              .setRequestMetadata(requestMetadata)
              .setOperationName(operation.getName())
              .setDone(operation.getDone())
              .setClusterId(clusterId)
              .build();
      if (operation.getDone() && operation.getResponse().is(ExecuteResponse.class)) {
        operationRequestMetadata =
            operationRequestMetadata.toBuilder()
                .setExecuteResponse(operation.getResponse().unpack(ExecuteResponse.class))
                .build();
        operationExitCodeCounter(
                Code.forNumber(operationRequestMetadata.getExecuteResponse().getStatus().getCode())
                    .name())
            .increment();
        if (operationRequestMetadata.getExecuteResponse().hasResult()
            && operationRequestMetadata.getExecuteResponse().getResult().hasExecutionMetadata()) {
          ExecutedActionMetadata executionMetadata =
              operationRequestMetadata.getExecuteResponse().getResult().getExecutionMetadata();
          operationsPerWorkerCounter(executionMetadata.getWorker()).increment();
          queuedTime.record(
              Time.toDurationMs(
                  executionMetadata.getQueuedTimestamp(),
                  executionMetadata.getExecutionStartTimestamp()),
              TimeUnit.MILLISECONDS);
          outputUploadTime.record(
              Time.toDurationMs(
                  executionMetadata.getOutputUploadStartTimestamp(),
                  executionMetadata.getOutputUploadCompletedTimestamp()),
              TimeUnit.MILLISECONDS);
        }
      }
      if (operation.getMetadata().is(ExecuteOperationMetadata.class)) {
        operationRequestMetadata =
            operationRequestMetadata.toBuilder()
                .setExecuteOperationMetadata(
                    operation.getMetadata().unpack(ExecuteOperationMetadata.class))
                .build();
        operationsInStageCounter(
                operationRequestMetadata.getExecuteOperationMetadata().getStage().name())
            .increment();
      }
      return operationRequestMetadata;
    } catch (Exception e) {
      log.log(
          Level.WARNING,
          String.format("Could not populate request metadata for %s.", operation.getName()),
          e);
      return null;
    }
  }

  protected static String formatRequestMetadataToJson(
      OperationRequestMetadata operationRequestMetadata) throws InvalidProtocolBufferException {
    JsonFormat.TypeRegistry typeRegistry =
        JsonFormat.TypeRegistry.newBuilder()
            .add(ExecuteResponse.getDescriptor())
            .add(ExecuteOperationMetadata.getDescriptor())
            .add(PreconditionFailure.getDescriptor())
            .build();

    String formattedRequestMetadata =
        JsonFormat.printer()
            .usingTypeRegistry(typeRegistry)
            .omittingInsignificantWhitespace()
            .print(operationRequestMetadata);
    log.log(Level.FINER, "{}", formattedRequestMetadata);
    return formattedRequestMetadata;
  }
}
