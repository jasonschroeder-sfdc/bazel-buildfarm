---
layout: default
title: Metrics
has_children: true
nav_order: 7
---

## Prometheus Configuration

To enable emitting of Prometheus metrics, add the following configuration to your configuration file:

```
server:
  prometheusPort: 9090
```

## Available Prometheus Metrics

### Server Metrics

**remote_invocations**

Counter for the number of invocations of the capabilities service

**execution_success**

Counter for the number of executions requests received

**merged_executions**

Counter for the number of executions merged by action

**pre_queue_size**

Gauge of a number of items in prequeue

**cas_miss**

Counter for number of CAS misses from worker-worker

**queue_failure**

Counter for number of operations that failed to queue

**requeue_failure**

Counter for number of operations that failed to requeue

**dispatched_operations_size**

Gauge of the number of dispatched operations





**worker_pool_size**

Gauge of the number of workers available

**storage_worker_pool_size**

Gauge of the number of storage workers available

**execute_worker_pool_size**

Gauge of the number of execute workers available.

**queue_size**

Gauge of the size of the queue (using a `queue_name` label for each individual queue)

**actions**

Counter for the number of actions processed

**operations_stage_load**

Counter for the number of operations in each stage (using a `stage_name` label for each individual stage)

**operation_status**

Counter for the completed operations status (using a `code` label for each individual [GRPC status code](https://grpc.github.io/grpc/core/md_doc_statuscodes.html))

**operation_exit_code**

Counter for the completed operations exit code (using an `exit_code` label for each individual execution exit code)

**operation_worker**

Counter for the number of operations executed on each worker (using a `worker_name` label for each individual worker)
#### Action Cache

**action_results**

Counter for the number of action results

**action_result_kind**

Counter for the action result response kind (using a `kind` label: hit, miss, or code)

** action_results_cancelled**

Counter for action result requests that were cancelled by the remote execution client (for example, bazel). These don't imply anything is wrong and may be part of a normal lifecycle.

#### CAS
**missing_blobs**

Histogram tracking the count of blobs requested via FindMissingBlobs RPC that were not found in the CAS. This measures how many blobs from each client request are missing from storage, helping identify cache effectiveness and potential upload needs. Uses exponential buckets to categorize request sizes

#### Remote Execution
**queued_time_s**

Histogram for the operation queued time (in seconds). Lower is better.

**output_upload_time_s**

Histogram for the output upload time (in seconds). Lower is better.

### Worker Metrics

Some of these are only for execution-workers.

**health_check**

Counter showing service health check events (using a `lifecycle` label)

**worker_paused**

Counter for worker pause events

**execution_slots_total**

Gauge of total execution slots configured on worker

**execution_slot_usage**

Gauge for the number of execution slots used on each worker. Should never exceed `execution_slots_total`.

**execution_time_ms**

Histogram for the execution time on a worker (in milliseconds)

**input_fetch_slots_total**

Gauge of total input fetch slots configured on worker

**input_fetch_slot_usage**

Gauge for the number of input fetch slots used on each worker. Should never exceed `input_fetch_slots_total`.

**input_fetch_time_ms**

Histogram for the input fetch time on a worker (in milliseconds)

**report_result_slots_total**

Gauge of total report result slots configured on worker

**report_result_slot_usage**

Gauge for the number of report result slots used on each worker. Should never exceed `report_result_slots_total`.

**report_result_time_ms**

Histogram for the report result time on a worker (in milliseconds)

**completed_operations**

Counter for the number of completed operations

**operation_poller**

Counter for the number of operations being polled

**zstd_buffer_pool_used**

Gauge of current number of Zstd decompression buffers active. This should not be growing without bound and should be "low" for Workers.

**local_resource_usage**

Gauge of number of claims for each resource currently being used for execution (using a `resource_name` label)

**local_resource_total**

Gauge of total number of claims that exist for a particular resource (using a `resource_name` label)

**local_resource_requesters**

Gauge tracking how many actions have requested local resources (using a `resource_name` label)

### CAS (Content Addressable Storage) Metrics

**expired_key**

Counter for the number of CAS entries that have been evicted from storage due to expiration. This tracks blobs that were removed because their TTL (time-to-live) expired, not due to LRU eviction or manual deletion.

**cas_size**

Gauge of total size of the worker's CAS in bytes

**cas_entry_count**

Gauge of the total number of entries in the worker's CAS

**cas_copy_fallback**

Counter for the number of times the CAS performed a file copy because hardlinking failed. On UNIX-based systems, this should be zero in a healthy Buildfarm.

**read_io_errors**

Counter for read I/O errors (Workers only). Greater than zero may indicate storage failure.

**cas_indexer_removed_keys**

Gauge of indexer results showing number of keys removed (using a `node` label)

**cas_indexer_removed_hosts**

Gauge of indexer results showing number of hosts removed (using a `node` label)

### I/O Metrics

**io_bytes_read**

Counter for bytes read from I/O operations

**io_bytes_write**

Histogram for bytes written to I/O operations (custom buckets: 10, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000)

Java interceptors can be used to monitor Grpc services using Prometheus.  To enable [these metrics](https://github.com/grpc-ecosystem/java-grpc-prometheus), add the following configuration to your server:
```
server:
  grpcMetrics:
    enabled: true
    provideLatencyHistograms: false
```
