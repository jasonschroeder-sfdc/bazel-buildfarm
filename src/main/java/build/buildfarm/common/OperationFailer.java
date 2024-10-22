// Copyright 2023 The Buildfarm Authors. All rights reserved.
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

package build.buildfarm.common;

import build.bazel.remote.execution.v2.ExecuteOperationMetadata;
import build.bazel.remote.execution.v2.ExecuteResponse;
import build.bazel.remote.execution.v2.ExecutedActionMetadata;
import build.bazel.remote.execution.v2.ExecutionStage;
import build.buildfarm.v1test.ExecuteEntry;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.rpc.Status;

/**
 * @class OperationFailer
 * @brief Converts any operation into a failed operation.
 * @details Sets properties on the existing operation so that the new operation is considered
 *     finished and failed.
 */
public class OperationFailer {
  public static Operation get(
      Operation operation,
      ExecuteEntry executeEntry,
      ExecutedActionMetadata partialExecutionMetadata,
      Status status) {
    return operation.toBuilder()
        .setDone(true)
        .setName(executeEntry.getOperationName())
        .setMetadata(
            Any.pack(
                executeOperationMetadata(
                    executeEntry, partialExecutionMetadata, ExecutionStage.Value.COMPLETED)))
        .setResponse(Any.pack(ExecuteResponse.newBuilder().setStatus(status).build()))
        .build();
  }

  private static ExecuteOperationMetadata executeOperationMetadata(
      ExecuteEntry executeEntry,
      ExecutedActionMetadata partialExecutionMetadata,
      ExecutionStage.Value stage) {
    return ExecuteOperationMetadata.newBuilder()
        .setActionDigest(executeEntry.getActionDigest())
        .setStdoutStreamName(executeEntry.getStdoutStreamName())
        .setStderrStreamName(executeEntry.getStderrStreamName())
        .setStage(stage)
        .setPartialExecutionMetadata(partialExecutionMetadata)
        .setDigestFunction(actionDigest.getDigestFunction())
        .build();
  }
}
