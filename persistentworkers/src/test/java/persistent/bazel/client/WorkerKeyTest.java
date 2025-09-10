package persistent.bazel.client;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WorkerKeyTest {

  private static final ImmutableList<String> DEFAULT_CMD =
      ImmutableList.of("java", "-jar", "worker.jar");
  private static final ImmutableList<String> DEFAULT_ARGS = ImmutableList.of("--arg1", "value1");
  private static final ImmutableMap<String, String> DEFAULT_ENV =
      ImmutableMap.of("PATH", "/usr/bin", "HOME", "/home/user");
  private static final String DEFAULT_MNEMONIC = "TestWorker";
  private static final boolean DEFAULT_SANDBOXED = true;
  private static final boolean DEFAULT_CANCELLABLE = false;

  private static final ImmutableList<String> DEFAULT_WRAPPER_ARGS =
      ImmutableList.of("--wrapper", "arg");
  private static final Path DEFAULT_EXEC_ROOT = Paths.get("/tmp/exec");
  private static final HashCode DEFAULT_HASH =
      Hashing.sha256().hashString("test", java.nio.charset.StandardCharsets.UTF_8);
  private static final SortedMap<Path, HashCode> DEFAULT_WORKER_FILES = createDefaultWorkerFiles();

  private static SortedMap<Path, HashCode> createDefaultWorkerFiles() {
    SortedMap<Path, HashCode> files = new TreeMap<>();
    files.put(Paths.get("worker.jar"), DEFAULT_HASH);
    files.put(Paths.get("config.properties"), DEFAULT_HASH);
    return files;
  }

  private static final UserPrincipal TEST_USER =
      new UserPrincipal() {
        @Override
        public String getName() {
          return "testuser";
        }

        @Override
        public boolean equals(Object obj) {
          return obj instanceof UserPrincipal && getName().equals(((UserPrincipal) obj).getName());
        }

        @Override
        public int hashCode() {
          return getName().hashCode();
        }
      };

  @Test
  public void testConstructorWithValidParameters() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    WorkerKey key =
        new WorkerKey(
            basicKey,
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertNotNull(key);
    assertEquals(basicKey, key.getBasicWorkerKey());
    assertEquals(TEST_USER, key.getOwner());
    assertEquals(DEFAULT_WRAPPER_ARGS, key.getWrapperArguments());
    assertEquals(DEFAULT_EXEC_ROOT, key.getExecRoot());
    assertEquals(DEFAULT_HASH, key.getWorkerFilesCombinedHash());
    assertEquals(DEFAULT_WORKER_FILES, key.getWorkerFilesWithHashes());
    assertEquals(DEFAULT_EXEC_ROOT.resolve(DEFAULT_HASH.toString()), key.getToolRoot());
  }

  @Test
  public void testConstructorWithNullOwner() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    WorkerKey key =
        new WorkerKey(
            basicKey,
            null,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertNotNull(key);
    assertEquals(basicKey, key.getBasicWorkerKey());
    assertNull(key.getOwner());
    assertEquals(DEFAULT_WRAPPER_ARGS, key.getWrapperArguments());
    assertEquals(DEFAULT_EXEC_ROOT, key.getExecRoot());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullBasicWorkerKey() {
    new WorkerKey(
        null,
        TEST_USER,
        DEFAULT_WRAPPER_ARGS,
        DEFAULT_EXEC_ROOT,
        DEFAULT_HASH,
        DEFAULT_WORKER_FILES);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullWrapperArguments() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    new WorkerKey(basicKey, TEST_USER, null, DEFAULT_EXEC_ROOT, DEFAULT_HASH, DEFAULT_WORKER_FILES);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullExecRoot() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    new WorkerKey(
        basicKey, TEST_USER, DEFAULT_WRAPPER_ARGS, null, DEFAULT_HASH, DEFAULT_WORKER_FILES);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullWorkerFilesCombinedHash() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    new WorkerKey(
        basicKey, TEST_USER, DEFAULT_WRAPPER_ARGS, DEFAULT_EXEC_ROOT, null, DEFAULT_WORKER_FILES);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullWorkerFilesWithHashes() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    new WorkerKey(basicKey, TEST_USER, DEFAULT_WRAPPER_ARGS, DEFAULT_EXEC_ROOT, DEFAULT_HASH, null);
  }

  @Test
  public void testConstructorWithEmptyCollections() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    ImmutableList<String> emptyWrapperArgs = ImmutableList.of();
    SortedMap<Path, HashCode> emptyWorkerFiles = new TreeMap<>();

    WorkerKey key =
        new WorkerKey(
            basicKey,
            TEST_USER,
            emptyWrapperArgs,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            emptyWorkerFiles);

    assertNotNull(key);
    assertEquals(emptyWrapperArgs, key.getWrapperArguments());
    assertEquals(emptyWorkerFiles, key.getWorkerFilesWithHashes());
  }

  @Test
  public void testEqualsWithSameObject() {
    WorkerKey key = createDefaultWorkerKey();
    assertTrue(key.equals(key));
  }

  @Test
  public void testEqualsWithNull() {
    WorkerKey key = createDefaultWorkerKey();
    assertFalse(key.equals(null));
  }

  @Test
  public void testEqualsWithDifferentClass() {
    WorkerKey key = createDefaultWorkerKey();
    assertFalse(key.equals("not a WorkerKey"));
  }

  @Test
  public void testEqualsWithIdenticalKeys() {
    WorkerKey key1 = createDefaultWorkerKey();
    WorkerKey key2 = createDefaultWorkerKey();

    assertTrue(key1.equals(key2));
    assertTrue(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentBasicWorkerKey() {
    WorkerKey key1 = createDefaultWorkerKey();

    BasicWorkerKey differentBasicKey =
        new BasicWorkerKey(
            ImmutableList.of("different", "cmd"),
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    WorkerKey key2 =
        new WorkerKey(
            differentBasicKey,
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentOwner() {
    WorkerKey key1 = createDefaultWorkerKey();

    UserPrincipal differentUser =
        new UserPrincipal() {
          @Override
          public String getName() {
            return "differentuser";
          }

          @Override
          public boolean equals(Object obj) {
            return obj instanceof UserPrincipal
                && getName().equals(((UserPrincipal) obj).getName());
          }

          @Override
          public int hashCode() {
            return getName().hashCode();
          }
        };

    WorkerKey key2 =
        new WorkerKey(
            createDefaultBasicWorkerKey(),
            differentUser,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testEqualsWithNullOwners() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();

    WorkerKey key1 =
        new WorkerKey(
            basicKey,
            null,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    WorkerKey key2 =
        new WorkerKey(
            basicKey,
            null,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertTrue(key1.equals(key2));
    assertTrue(key2.equals(key1));
  }

  @Test
  public void testEqualsWithOneNullOwner() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();

    WorkerKey key1 =
        new WorkerKey(
            basicKey,
            null,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    WorkerKey key2 =
        new WorkerKey(
            basicKey,
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentWrapperArguments() {
    WorkerKey key1 = createDefaultWorkerKey();

    WorkerKey key2 =
        new WorkerKey(
            createDefaultBasicWorkerKey(),
            TEST_USER,
            ImmutableList.of("--different", "wrapper"),
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentExecRoot() {
    WorkerKey key1 = createDefaultWorkerKey();

    WorkerKey key2 =
        new WorkerKey(
            createDefaultBasicWorkerKey(),
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            Paths.get("/different/exec/root"),
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testHashCodeConsistency() {
    WorkerKey key = createDefaultWorkerKey();
    int hash1 = key.hashCode();
    int hash2 = key.hashCode();

    assertEquals("hashCode should be consistent", hash1, hash2);
  }

  @Test
  public void testHashCodeEqualityContract() {
    WorkerKey key1 = createDefaultWorkerKey();
    WorkerKey key2 = createDefaultWorkerKey();

    assertTrue(
        "Equal objects must have equal hash codes",
        key1.equals(key2) && key1.hashCode() == key2.hashCode());
  }

  @Test
  public void testHashCodeWithDifferentObjects() {
    WorkerKey key1 = createDefaultWorkerKey();

    WorkerKey key2 =
        new WorkerKey(
            createDefaultBasicWorkerKey(),
            TEST_USER,
            ImmutableList.of("--different", "wrapper"),
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    // Different objects should typically have different hash codes
    assertNotEquals(
        "Different objects should typically have different hash codes",
        key1.hashCode(),
        key2.hashCode());
  }

  @Test
  public void testGetters() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    UserPrincipal owner = TEST_USER;
    ImmutableList<String> wrapperArgs = ImmutableList.of("--test", "wrapper");
    Path execRoot = Paths.get("/test/exec");
    HashCode combinedHash =
        Hashing.sha256().hashString("test123", java.nio.charset.StandardCharsets.UTF_8);
    SortedMap<Path, HashCode> workerFiles = new TreeMap<>();
    workerFiles.put(Paths.get("test.jar"), combinedHash);

    WorkerKey key =
        new WorkerKey(basicKey, owner, wrapperArgs, execRoot, combinedHash, workerFiles);

    assertEquals(basicKey, key.getBasicWorkerKey());
    assertEquals(owner, key.getOwner());
    assertEquals(wrapperArgs, key.getWrapperArguments());
    assertEquals(execRoot, key.getExecRoot());
    assertEquals(combinedHash, key.getWorkerFilesCombinedHash());
    assertEquals(workerFiles, key.getWorkerFilesWithHashes());
    assertEquals(execRoot.resolve(combinedHash.toString()), key.getToolRoot());
  }

  @Test
  public void testDelegationMethods() {
    BasicWorkerKey basicKey = createDefaultBasicWorkerKey();
    WorkerKey key =
        new WorkerKey(
            basicKey,
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            DEFAULT_EXEC_ROOT,
            DEFAULT_HASH,
            DEFAULT_WORKER_FILES);

    // Test delegation to BasicWorkerKey methods
    assertEquals(basicKey.getArgs(), key.getArgs());
    assertEquals(basicKey.getCmd(), key.getCmd());
    assertEquals(basicKey.getEnv(), key.getEnv());
    assertEquals(basicKey.getMnemonic(), key.getMnemonic());
    assertEquals(basicKey.isCancellable(), key.isCancellable());
    assertEquals(basicKey.isSandboxed(), key.isSandboxed());
  }

  @Test
  public void testToString() {
    WorkerKey key = createDefaultWorkerKey();
    String toString = key.toString();

    assertNotNull(toString);
    assertTrue("toString should contain basicWorkerKey", toString.contains("basicWorkerKey"));
    assertTrue("toString should contain execRoot", toString.contains("execRoot"));
  }

  @Test
  public void testImmutability() {
    WorkerKey key = createDefaultWorkerKey();

    // Verify that getters return immutable collections
    assertTrue(
        "wrapperArguments should be immutable", key.getWrapperArguments() instanceof ImmutableList);

    // Verify delegation methods return immutable collections
    assertTrue("args should be immutable", key.getArgs() instanceof ImmutableList);
    assertTrue("cmd should be immutable", key.getCmd() instanceof ImmutableList);
    assertTrue("env should be immutable", key.getEnv() instanceof ImmutableMap);
  }

  @Test
  public void testToolRootDerivation() {
    Path execRoot = Paths.get("/custom/exec/root");
    HashCode hash = Hashing.sha256().hashString("custom", java.nio.charset.StandardCharsets.UTF_8);

    WorkerKey key =
        new WorkerKey(
            createDefaultBasicWorkerKey(),
            TEST_USER,
            DEFAULT_WRAPPER_ARGS,
            execRoot,
            hash,
            DEFAULT_WORKER_FILES);

    Path expectedToolRoot = execRoot.resolve(hash.toString());
    assertEquals(
        "toolRoot should be derived from execRoot and hash", expectedToolRoot, key.getToolRoot());
  }

  private BasicWorkerKey createDefaultBasicWorkerKey() {
    return new BasicWorkerKey(
        DEFAULT_CMD,
        DEFAULT_ARGS,
        DEFAULT_ENV,
        DEFAULT_MNEMONIC,
        DEFAULT_SANDBOXED,
        DEFAULT_CANCELLABLE);
  }

  private WorkerKey createDefaultWorkerKey() {
    return new WorkerKey(
        createDefaultBasicWorkerKey(),
        TEST_USER,
        DEFAULT_WRAPPER_ARGS,
        DEFAULT_EXEC_ROOT,
        DEFAULT_HASH,
        DEFAULT_WORKER_FILES);
  }
}
