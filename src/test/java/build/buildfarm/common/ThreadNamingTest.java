// Copyright 2025 The Buildfarm Authors. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** Tests for thread naming utilities and verification. */
public class ThreadNamingTest {

  @Test
  public void testThreadFactoryUtilsCreatesNamedThreads() throws Exception {
    ThreadFactory factory = ThreadFactoryUtils.createNamedThreadFactory("TestThread-%d");
    Thread thread1 = factory.newThread(() -> {});
    Thread thread2 = factory.newThread(() -> {});

    assertEquals("TestThread-0", thread1.getName());
    assertEquals("TestThread-1", thread2.getName());
  }

  @Test
  public void testThreadFactoryUtilsCreatesSingleNamedThread() throws Exception {
    ThreadFactory factory = ThreadFactoryUtils.createNamedSingleThreadFactory("SingleThread");
    Thread thread = factory.newThread(() -> {});

    assertEquals("SingleThread", thread.getName());
  }

  @Test
  public void testExecutorServiceWithNamedThreadFactory() throws Exception {
    ExecutorService executor =
        Executors.newSingleThreadExecutor(
            ThreadFactoryUtils.createNamedSingleThreadFactory("TestExecutor"));

    Future<?> future =
        executor.submit(
            () -> {
              assertEquals("TestExecutor", Thread.currentThread().getName());
            });

    future.get(1, TimeUnit.SECONDS);
    executor.shutdown();
    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
  }

  @Test
  public void testFixedThreadPoolWithNamedThreadFactory() throws Exception {
    ExecutorService executor =
        Executors.newFixedThreadPool(
            3, ThreadFactoryUtils.createNamedThreadFactory("PoolThread-%d"));

    Set<String> threadNames = new HashSet<>();
    Set<Future<?>> futures = new HashSet<>();

    for (int i = 0; i < 3; i++) {
      Future<?> future =
          executor.submit(
              () -> {
                synchronized (threadNames) {
                  threadNames.add(Thread.currentThread().getName());
                }
              });
      futures.add(future);
    }

    for (Future<?> future : futures) {
      future.get(1, TimeUnit.SECONDS);
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

    assertEquals(3, threadNames.size());
    assertTrue(threadNames.contains("PoolThread-0"));
    assertTrue(threadNames.contains("PoolThread-1"));
    assertTrue(threadNames.contains("PoolThread-2"));
  }

  @Test
  public void testThreadNamesAreNotDefault() {
    ThreadFactory factory = ThreadFactoryUtils.createNamedThreadFactory("CustomThread-%d");
    Thread thread = factory.newThread(() -> {});

    String name = thread.getName();
    assertNotNull(name);
    assertFalse(name.startsWith("Thread-"));
    assertFalse(name.startsWith("pool-"));
    assertTrue(name.startsWith("CustomThread-"));
  }

  @Test
  public void testThreadNameUniqueness() {
    ThreadFactory factory = ThreadFactoryUtils.createNamedThreadFactory("UniqueThread-%d");
    Set<String> names = new HashSet<>();

    for (int i = 0; i < 10; i++) {
      Thread thread = factory.newThread(() -> {});
      String name = thread.getName();
      assertFalse("Duplicate thread name: " + name, names.contains(name));
      names.add(name);
    }

    assertEquals(10, names.size());
  }
}

