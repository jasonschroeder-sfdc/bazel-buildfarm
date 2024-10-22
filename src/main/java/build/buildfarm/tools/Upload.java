package build.buildfarm.tools;

import static build.bazel.remote.execution.v2.Compressor.Value.ZSTD;
import static build.buildfarm.common.grpc.Channels.createChannel;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.RequestMetadata;
import build.buildfarm.common.DigestUtil;
import build.buildfarm.common.Write;
import build.buildfarm.common.ZstdCompressingInputStream;
import build.buildfarm.instance.Instance;
import build.buildfarm.instance.stub.StubInstance;
import com.google.common.io.ByteStreams;
import com.google.protobuf.util.Durations;
import io.grpc.ManagedChannel;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class Upload {
  private static void main(String host, String instanceName, DigestUtil digestUtil, Path path)
      throws Exception {
    ManagedChannel channel = createChannel(host);
    Instance instance =
        new StubInstance(instanceName, "bf-upload", digestUtil, channel, Durations.fromDays(10));

    Digest digest = digestUtil.compute(path);
    Write write =
        instance.getBlobWrite(
            ZSTD,
            digest,
            digestUtil.getDigestFunction(),
            UUID.randomUUID(),
            RequestMetadata.getDefaultInstance());
    try (OutputStream out = write.getOutput(0l, 10, TimeUnit.DAYS, () -> {});
        InputStream in = new ZstdCompressingInputStream(Files.newInputStream(path))) {
      ByteStreams.copy(in, out);
    }
    write.getFuture().get();
    System.out.println("Completed uploading " + DigestUtil.toString(digest));
  }

  public static void main(String[] args) throws Exception {
    String host = args[0];
    String instanceName = args[1];
    DigestUtil digestUtil = DigestUtil.forHash(args[2]);
    Path path = Paths.get(args[3]);
    main(host, instanceName, digestUtil, path);
  }
}
