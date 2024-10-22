package build.buildfarm.instance.shard;

import static com.google.common.truth.Truth.assertThat;

import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.common.DigestUtil;
import com.github.fppt.jedismock.RedisServer;
import com.github.fppt.jedismock.server.ServiceOptions;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

@RunWith(JUnit4.class)
public class JedisCasWorkerMapTest {
  private static final String CAS_PREFIX = "ContentAddressableStorage";

  private RedisServer redisServer;
  private JedisCluster jedis;
  private JedisCasWorkerMap jedisCasWorkerMap;

  @Before
  public void setup() throws IOException {
    redisServer =
        RedisServer.newRedisServer(0, InetAddress.getByName("localhost"))
            .setOptions(ServiceOptions.defaultOptions().withClusterModeEnabled())
            .start();
    jedis =
        new JedisCluster(
            Collections.singleton(
                new HostAndPort(redisServer.getHost(), redisServer.getBindPort())));
    jedisCasWorkerMap = new JedisCasWorkerMap(jedis, CAS_PREFIX, 60);
  }

  @Test
  public void testSetExpire() throws IOException {
    Digest testDigest1 = Digest.newBuilder().setHash("abc").build();
    Digest testDigest2 = Digest.newBuilder().setHash("xyz").build();

    String casKey1 = CAS_PREFIX + ":" + DigestUtil.toString(testDigest1);
    String casKey2 = CAS_PREFIX + ":" + DigestUtil.toString(testDigest2);

    jedis.sadd(casKey1, "worker1");
    jedisCasWorkerMap.setExpire(Arrays.asList(testDigest1, testDigest2));

    assertThat(jedis.ttl(casKey1)).isGreaterThan(0);
    assertThat(jedis.ttl(casKey2)).isEqualTo(-2);
  }

  @After
  public void tearDown() throws IOException {
    redisServer.stop();
  }
}
