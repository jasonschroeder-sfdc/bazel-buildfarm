// Copyright 2017 The Bazel Authors. All rights reserved.
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

package build.buildfarm.worker;

import com.google.common.collect.Sets;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import lombok.extern.java.Log;

@Log
public class InputFetchStage extends SuperscalarPipelineStage {
  private final Set<Thread> fetchers = Sets.newHashSet();
  private final BlockingQueue<OperationContext> queue = new ArrayBlockingQueue<>(1);

  private final Gauge inputFetchSlotUsage =
      Gauge.builder("input.fetch.slot.usage", this::getSlotUsage)
          .description("Input fetch slot Usage.")
          .register(Metrics.globalRegistry);
  private final Timer inputFetchTime =
      Timer.builder("input.fetch").description("Input fetch").register(Metrics.globalRegistry);
  private final Timer inputFetchStallTime =
      Timer.builder("input.fetch.stall")
          .description("Input fetch stall time")
          .register(Metrics.globalRegistry);

  public InputFetchStage(WorkerContext workerContext, PipelineStage output, PipelineStage error) {
    super("InputFetchStage", workerContext, output, error, workerContext.getInputFetchStageWidth());
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  public OperationContext take() throws InterruptedException {
    return takeOrDrain(queue);
  }

  @Override
  public void put(OperationContext operationContext) throws InterruptedException {
    queue.put(operationContext);
  }

  synchronized int removeAndRelease(String operationName) {
    if (!fetchers.remove(Thread.currentThread())) {
      throw new IllegalStateException("tried to remove unknown fetcher thread");
    }
    releaseClaim(operationName, 1);
    int slotUsage = fetchers.size();
    return slotUsage;
  }

  public void releaseInputFetcher(
      String operationName, long usecs, long stallUSecs, boolean success) {
    int size = removeAndRelease(operationName);
    inputFetchTime.record(usecs, TimeUnit.MICROSECONDS);
    inputFetchStallTime.record(stallUSecs, TimeUnit.MICROSECONDS);
    complete(
        operationName,
        usecs,
        stallUSecs,
        String.format("%s, %s", success ? "Success" : "Failure", getUsage(size)));
  }

  @Override
  public int getSlotUsage() {
    return fetchers.size();
  }

  @Override
  protected synchronized void interruptAll() {
    for (Thread fetcher : fetchers) {
      fetcher.interrupt();
    }
  }

  @Override
  protected int claimsRequired(OperationContext operationContext) {
    return 1;
  }

  @Override
  protected void iterate() throws InterruptedException {
    OperationContext operationContext = take();
    Thread fetcher =
        new Thread(
            new InputFetcher(workerContext, operationContext, this), "InputFetchStage.fetcher");

    synchronized (this) {
      fetchers.add(fetcher);
      int slotUsage = fetchers.size();
      start(operationContext.queueEntry.getExecuteEntry().getOperationName(), getUsage(slotUsage));
      fetcher.start();
    }
  }
}
