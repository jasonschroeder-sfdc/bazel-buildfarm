// Copyright 2019 The Buildfarm Authors. All rights reserved.
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

package build.buildfarm.instance.shard;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import build.buildfarm.common.grpc.Channels;
import build.buildfarm.common.grpc.Retrier;
import build.buildfarm.common.grpc.Retrier.Backoff;
import build.buildfarm.instance.Instance;
import build.buildfarm.instance.stub.StubInstance;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Duration;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorkerStubs {
  private WorkerStubs() {}

  public static LoadingCache<String, StubInstance> create(Duration timeout) {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build(
            new CacheLoader<>() {
              @SuppressWarnings("NullableProblems")
              @Override
              public StubInstance load(String worker) {
                return newStubInstance(worker, timeout);
              }
            });
  }

  private static ManagedChannel createChannel(
      String worker, AtomicInteger active, Runnable onAllIdle) {
    ManagedChannel channel = Channels.createChannel(worker);
    channel.notifyWhenStateChanged(
        channel.getState(/* requestConnection= */ false),
        new Runnable() {
          boolean wasIdle = false;

          @Override
          public void run() {
            ConnectivityState state = channel.getState(/* requestConnection= */ false);
            if (state == ConnectivityState.IDLE) {
              if (active.decrementAndGet() == 0) {
                onAllIdle.run();
              }
              wasIdle = true;
            } else if (wasIdle) {
              wasIdle = false;
              active.incrementAndGet();
            }
            channel.notifyWhenStateChanged(state, this);
          }
        });
    return channel;
  }

  private static StubInstance newStubInstance(String worker, Duration timeout) {
    AtomicInteger active = new AtomicInteger(2); // one for each channel
    SettableFuture<Void> idle = SettableFuture.create();
    Runnable onAllIdle = () -> idle.set(null);
    ManagedChannel channel = createChannel(worker, active, onAllIdle);
    ManagedChannel writeChannel = createChannel(worker, active, onAllIdle);
    StubInstance instance =
        new StubInstance(
            "",
            worker,
            channel,
            writeChannel, // separate write channel
            timeout,
            newStubRetrier(),
            newStubRetryService());
    idle.addListener(() -> stopInstance(instance), directExecutor());
    return instance;
  }

  private static Retrier newStubRetrier() {
    return new Retrier(
        Backoff.exponential(
            java.time.Duration.ofMillis(/*options.experimentalRemoteRetryStartDelayMillis=*/ 100),
            java.time.Duration.ofMillis(/*options.experimentalRemoteRetryMaxDelayMillis=*/ 5000),
            /*options.experimentalRemoteRetryMultiplier=*/ 2,
            /*options.experimentalRemoteRetryJitter=*/ 0.1,
            /*options.experimentalRemoteRetryMaxAttempts=*/ 5),
        Retrier.DEFAULT_IS_RETRIABLE);
  }

  private static ListeningScheduledExecutorService newStubRetryService() {
    return listeningDecorator(newSingleThreadScheduledExecutor());
  }

  private static void stopInstance(Instance instance) {
    try {
      instance.stop();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
