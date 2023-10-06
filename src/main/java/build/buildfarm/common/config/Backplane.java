package build.buildfarm.common.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.redisson.misc.RedisURI;
import oshi.util.FileUtil;
import redis.clients.jedis.util.JedisURIHelper;

import javax.annotation.Nullable;
import javax.naming.ConfigurationException;
import java.net.URI;

@Data
public class Backplane {
  public enum BACKPLANE_TYPE {
    SHARD
  }

  private BACKPLANE_TYPE type = BACKPLANE_TYPE.SHARD;
  private String redisUri;
  private int jedisPoolMaxTotal = 4000;
  private String workersHashName = "Workers";
  private String workerChannel = "WorkerChannel";
  private String actionCachePrefix = "ActionCache";
  private int actionCacheExpire = 2419200; // 4 Weeks
  private String actionBlacklistPrefix = "ActionBlacklist";
  private int actionBlacklistExpire = 3600; // 1 Hour;
  private String invocationBlacklistPrefix = "InvocationBlacklist";
  private String operationPrefix = "Operation";
  private int operationExpire = 604800; // 1 Week
  private String preQueuedOperationsListName = "{Arrival}:PreQueuedOperations";
  private String processingListName = "{Arrival}:ProcessingOperations";
  private String processingPrefix = "Processing";
  private int processingTimeoutMillis = 20000;
  private String queuedOperationsListName = "{Execution}:QueuedOperations";
  private String dispatchingPrefix = "Dispatching";
  private int dispatchingTimeoutMillis = 10000;
  private String dispatchedOperationsHashName = "DispatchedOperations";
  private String operationChannelPrefix = "OperationChannel";
  private String casPrefix = "ContentAddressableStorage";
  private int casExpire = 604800; // 1 Week

  @Getter(AccessLevel.NONE)
  private boolean subscribeToBackplane = true; // deprecated

  @Getter(AccessLevel.NONE)
  private boolean runFailsafeOperation = true; // deprecated

  private int maxQueueDepth = 100000;
  private int maxPreQueueDepth = 1000000;
  private boolean priorityQueue = false;
  private Queue[] queues = {};
  private String redisPassword;
  private String redisCredentialFile;
  private int timeout = 10000;
  private String[] redisNodes = {};
  private int maxAttempts = 20;
  private boolean cacheCas = false;
  private long priorityPollIntervalMillis = 100;

  // These limited resources are shared across all workers.
  // An example would be a limited number of seats to a license server.
  private List<LimitedResource> resources = new ArrayList<>();

  public String getRedisUri() {
    // use environment override (useful for containerized deployment)
    if (!Strings.isNullOrEmpty(System.getenv("REDIS_URI"))) {
      return System.getenv("REDIS_URI");
    }

    // use configured value
    return redisUri;
  }

  /**
   * Look in several prioritized ways to get a Redis password:
   * <ol>
   *     <l><c>REDIS_PASSWORD</c> env var (Highest priority)</li>
   *     <li>the password in the Redis URI (wherever that came from)</li>
   *     <li>The `redisPassword` from config YAML </li>
   *     <li>the `redisCredentialFile`.</li>
   * </ol>
   * @return The redis password, or <c>null</c> if unset.
   */
  public @Nullable String getRedisPassword() {
    if (!Strings.isNullOrEmpty(System.getenv("REDIS_PASSWORD"))) {
      return System.getenv("REDIS_PASSWORD");
    }
    URI redisProperUri = URI.create(getRedisUri());
    if (!Strings.isNullOrEmpty(JedisURIHelper.getPassword(redisProperUri))) {
      return JedisURIHelper.getPassword(redisProperUri);
    }

    if (!Strings.isNullOrEmpty(redisCredentialFile)) {
      //Finally, get the password from the config file.
      return FileUtil.getStringFromFile(redisCredentialFile);
    }
    
    return Strings.emptyToNull(redisPassword);
  }
}
