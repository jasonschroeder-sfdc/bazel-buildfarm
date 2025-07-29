package persistent.testutil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.HashCode;
import java.nio.file.Path;
import persistent.bazel.client.BasicWorkerKey;
import persistent.bazel.client.WorkerKey;

public class WorkerUtils {
  public static WorkerKey emptyWorkerKey(Path execDir, ImmutableList<String> initCmd) {
    return new WorkerKey(
        new BasicWorkerKey(
            initCmd,
            /* args= */ ImmutableList.of(),
            /* env= */ ImmutableMap.of(),
            /* mnemonic= */ "TestOp-Adder",
            false,
            false),
        /* owner= */ null,
        /* wrapperArguments= */ ImmutableList.of(),
        /* execRoot= */ execDir,
        /* workerFilesCombinedHash= */ HashCode.fromInt(0),
        /* workerFilesWithHashes= */ ImmutableSortedMap.of());
  }
}
