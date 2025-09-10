package persistent.bazel.client;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicWorkerKeyTest {

  private static final ImmutableList<String> DEFAULT_CMD =
      ImmutableList.of("java", "-jar", "worker.jar");
  private static final ImmutableList<String> DEFAULT_ARGS = ImmutableList.of("--arg1", "value1");
  private static final ImmutableMap<String, String> DEFAULT_ENV =
      ImmutableMap.of("PATH", "/usr/bin", "HOME", "/home/user");
  private static final String DEFAULT_MNEMONIC = "TestWorker";
  private static final boolean DEFAULT_SANDBOXED = true;
  private static final boolean DEFAULT_CANCELLABLE = false;

  @Test
  public void testConstructorWithValidParameters() {
    BasicWorkerKey key =
        new BasicWorkerKey(
            DEFAULT_CMD,
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertNotNull(key);
    assertEquals(DEFAULT_CMD, key.getCmd());
    assertEquals(DEFAULT_ARGS, key.getArgs());
    assertEquals(DEFAULT_ENV, key.getEnv());
    assertEquals(DEFAULT_MNEMONIC, key.getMnemonic());
    assertEquals(DEFAULT_SANDBOXED, key.isSandboxed());
    assertEquals(DEFAULT_CANCELLABLE, key.isCancellable());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullCmd() {
    new BasicWorkerKey(
        null, DEFAULT_ARGS, DEFAULT_ENV, DEFAULT_MNEMONIC, DEFAULT_SANDBOXED, DEFAULT_CANCELLABLE);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullArgs() {
    new BasicWorkerKey(
        DEFAULT_CMD, null, DEFAULT_ENV, DEFAULT_MNEMONIC, DEFAULT_SANDBOXED, DEFAULT_CANCELLABLE);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullEnv() {
    new BasicWorkerKey(
        DEFAULT_CMD, DEFAULT_ARGS, null, DEFAULT_MNEMONIC, DEFAULT_SANDBOXED, DEFAULT_CANCELLABLE);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullMnemonic() {
    new BasicWorkerKey(
        DEFAULT_CMD, DEFAULT_ARGS, DEFAULT_ENV, null, DEFAULT_SANDBOXED, DEFAULT_CANCELLABLE);
  }

  @Test
  public void testConstructorWithEmptyCollections() {
    ImmutableList<String> emptyCmd = ImmutableList.of();
    ImmutableList<String> emptyArgs = ImmutableList.of();
    ImmutableMap<String, String> emptyEnv = ImmutableMap.of();

    BasicWorkerKey key =
        new BasicWorkerKey(
            emptyCmd,
            emptyArgs,
            emptyEnv,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertNotNull(key);
    assertEquals(emptyCmd, key.getCmd());
    assertEquals(emptyArgs, key.getArgs());
    assertEquals(emptyEnv, key.getEnv());
  }

  @Test
  public void testEqualsWithSameObject() {
    BasicWorkerKey key = createDefaultKey();
    assertTrue(key.equals(key));
  }

  @Test
  public void testEqualsWithNull() {
    BasicWorkerKey key = createDefaultKey();
    assertFalse(key.equals(null));
  }

  @Test
  public void testEqualsWithDifferentClass() {
    BasicWorkerKey key = createDefaultKey();
    assertFalse(key.equals("not a BasicWorkerKey"));
  }

  @Test
  public void testEqualsWithIdenticalKeys() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 = createDefaultKey();

    assertTrue(key1.equals(key2));
    assertTrue(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentCmd() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            ImmutableList.of("different", "cmd"),
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
    assertFalse(key2.equals(key1));
  }

  @Test
  public void testEqualsWithDifferentArgs() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            DEFAULT_CMD,
            ImmutableList.of("--different", "args"),
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
  }

  @Test
  public void testEqualsWithDifferentEnv() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            DEFAULT_CMD,
            DEFAULT_ARGS,
            ImmutableMap.of("DIFFERENT", "env"),
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
  }

  @Test
  public void testEqualsWithDifferentMnemonic() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            DEFAULT_CMD,
            DEFAULT_ARGS,
            DEFAULT_ENV,
            "DifferentMnemonic",
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
  }

  @Test
  public void testEqualsWithDifferentSandboxed() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            DEFAULT_CMD,
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            !DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
  }

  @Test
  public void testEqualsWithDifferentCancellable() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            DEFAULT_CMD,
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            !DEFAULT_CANCELLABLE);

    assertFalse(key1.equals(key2));
  }

  @Test
  public void testHashCodeConsistency() {
    BasicWorkerKey key = createDefaultKey();
    int hash1 = key.hashCode();
    int hash2 = key.hashCode();

    assertEquals("hashCode should be consistent", hash1, hash2);
  }

  @Test
  public void testHashCodeEqualityContract() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 = createDefaultKey();

    assertTrue(
        "Equal objects must have equal hash codes",
        key1.equals(key2) && key1.hashCode() == key2.hashCode());
  }

  @Test
  public void testHashCodeWithDifferentObjects() {
    BasicWorkerKey key1 = createDefaultKey();
    BasicWorkerKey key2 =
        new BasicWorkerKey(
            ImmutableList.of("different", "cmd"),
            DEFAULT_ARGS,
            DEFAULT_ENV,
            DEFAULT_MNEMONIC,
            DEFAULT_SANDBOXED,
            DEFAULT_CANCELLABLE);

    // Different objects should typically have different hash codes
    // (though not guaranteed, this is a good practice test)
    assertNotEquals(
        "Different objects should typically have different hash codes",
        key1.hashCode(),
        key2.hashCode());
  }

  @Test
  public void testToString() {
    BasicWorkerKey key = createDefaultKey();
    String toString = key.toString();

    assertNotNull(toString);
    assertTrue("toString should contain cmd", toString.contains("cmd"));
    assertTrue("toString should contain args", toString.contains("args"));
    assertTrue("toString should contain env", toString.contains("env"));
    assertTrue("toString should contain mnemonic", toString.contains("mnemonic"));
    assertTrue("toString should contain the mnemonic value", toString.contains(DEFAULT_MNEMONIC));
  }

  @Test
  public void testGetters() {
    ImmutableList<String> cmd = ImmutableList.of("test", "command");
    ImmutableList<String> args = ImmutableList.of("--test", "arg");
    ImmutableMap<String, String> env = ImmutableMap.of("TEST_VAR", "test_value");
    String mnemonic = "TestMnemonic";
    boolean sandboxed = true;
    boolean cancellable = true;

    BasicWorkerKey key = new BasicWorkerKey(cmd, args, env, mnemonic, sandboxed, cancellable);

    assertEquals(cmd, key.getCmd());
    assertEquals(args, key.getArgs());
    assertEquals(env, key.getEnv());
    assertEquals(mnemonic, key.getMnemonic());
    assertEquals(sandboxed, key.isSandboxed());
    assertEquals(cancellable, key.isCancellable());
  }

  @Test
  public void testImmutability() {
    BasicWorkerKey key = createDefaultKey();

    // Verify that getters return immutable collections
    assertTrue("cmd should be immutable", key.getCmd() instanceof ImmutableList);
    assertTrue("args should be immutable", key.getArgs() instanceof ImmutableList);
    assertTrue("env should be immutable", key.getEnv() instanceof ImmutableMap);
  }

  private BasicWorkerKey createDefaultKey() {
    return new BasicWorkerKey(
        DEFAULT_CMD,
        DEFAULT_ARGS,
        DEFAULT_ENV,
        DEFAULT_MNEMONIC,
        DEFAULT_SANDBOXED,
        DEFAULT_CANCELLABLE);
  }
}
