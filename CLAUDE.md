# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Buildfarm is a remote caching and execution system implementing the [Remote Execution API](https://github.com/bazelbuild/remote-apis), compatible with Bazel, buck2, pants, and other build systems. It consists of a **Server** (coordination) and **Workers** (execution), coordinated through **Redis**.

## Development Commands

### Build
```bash
bazel build //...                                                          # all targets
bazel build //src/main/java/build/buildfarm:buildfarm-server               # server only
bazel build //src/main/java/build/buildfarm:buildfarm-shard-worker         # worker only
```

### Test
```bash
bazel test //...                                                           # all (excludes redis,integration by default)
bazel test //src/test/java/build/buildfarm/worker:WorkerTest               # specific test target
bazel test //src/test/java/build/buildfarm/worker:WorkerTest --test_filter=testMethodName  # single method
bazel test --test_tag_filters=redis //...                                  # redis-dependent tests
bazel test --test_tag_filters=integration //...                            # integration tests
```

### Lint and Format
```bash
./.bazelci/format.sh            # format all (google-java-format + buildifier + hawkeye license headers)
./.bazelci/format.sh --check    # CI check mode (fails on unformatted code)
```

The format script runs three tools: google-java-format for Java files, buildifier for BUILD/bzl files, and hawkeye for license headers.

### NullAway Static Analysis
```bash
bazel build --config=nullaway //...
bazel test --config=nullaway //...
```

### Re-pin Maven Dependencies
```bash
REPIN=1 bazel run @buildfarm_maven//:pin
```

### Running Locally
```bash
docker run -d --rm --name buildfarm-redis -p 6379:6379 redis:7.2.4        # start Redis
redis-cli config set stop-writes-on-bgsave-error no
./run_server                                                               # start server
./run_worker                                                               # start worker
```

## Architecture

### Entry Points
- **Server**: `build.buildfarm.server.BuildFarmServer` — target `//src/main/java/build/buildfarm:buildfarm-server`
- **Worker**: `build.buildfarm.worker.shard.Worker` — target `//src/main/java/build/buildfarm:buildfarm-shard-worker`

Both load YAML config via `BuildfarmConfigs` singleton (SnakeYAML, supports `!include` directives). Example configs in `examples/`.

### Key Packages (`src/main/java/build/buildfarm/`)

| Package | Responsibility |
|---------|---------------|
| `server/` | gRPC server setup, `BuildFarmServer` main class |
| `server/services/` | gRPC service implementations (Execution, ActionCache, CAS, ByteStream, Operations, Capabilities, Fetch, OperationQueue, WorkerProfile, PublishBuildEvent) |
| `worker/` | Execution pipeline stages and supporting classes |
| `worker/shard/` | Worker entry point, `ShardWorkerContext` wiring |
| `instance/` | `Instance` interface — core abstraction for all operations |
| `instance/shard/` | `ServerInstance` — server-side Instance backed by Redis backplane |
| `instance/stub/` | `StubInstance` — gRPC client stub for remote instances |
| `backplane/` | `Backplane` interface + `RedisShardBackplane` — distributed coordination via Redis |
| `cas/` | CAS interface, `MemoryCAS`, `GrpcCAS` |
| `cas/cfc/` | File-system CAS: `CASFileCache` (abstract), `DirectoryEntryCFC` (default), `LegacyDirectoryCFC` |
| `actioncache/` | `ActionCache` interface, `ShardActionCache` (Redis-backed) |
| `common/config/` | `BuildfarmConfigs` and YAML config POJOs (Lombok `@Data`) |
| `common/redis/` | Redis abstractions: `RedisClient`, `RedisQueue`, `BalancedRedisQueue`, `RedisMap` |
| `metrics/` | Prometheus metrics publisher |

### Worker Execution Pipeline

The worker processes actions through a 4-stage pipeline (`Pipeline.java`):

```
MatchStage → InputFetchStage → ExecuteActionStage → ReportResultStage
 (width=1)   (configurable)     (configurable)       (configurable)
```

1. **MatchStage** — Dequeues operations from Redis via `backplane.dispatchOperation()`, creates `ExecutionContext`
2. **InputFetchStage** — Downloads action inputs from CAS via `ExecFileSystem.createExecDir()`
3. **ExecuteActionStage** — Runs the build action (native process, Docker/Podman, or sandbox)
4. **ReportResultStage** — Uploads outputs to CAS, writes ActionResult to action cache, completes operation

Parallel stages extend `SuperscalarPipelineStage`. Resource management uses `LocalResourceSet` for CPU/memory claims.

### Operation Queue / Scheduling

Operations flow through: **Prequeue** → **Provision Queues** → **Dispatched**

- **Prequeue** (`BalancedRedisQueue`): Arrival queue with duplicate-action merging via `ActionKey`
- **Provision Queues** (`ProvisionedRedisQueue`): Platform-property-based routing (e.g., `os:linux` workers match `os:linux` queue). Uses Redis sorted sets for priority.
- **Dispatched**: Redis hash of running operations with keepalive polling. `DispatchedMonitor` requeues stale operations.

### Redis Usage

Uses **Jedis** (`redis.clients.jedis`) with `UnifiedJedis` (supports standalone and cluster mode).

Key patterns:
- **Hash tags** (`{Operations}:...`, `{Workers}:...`, `{CasMap}:...`) for cluster slot co-location (`RedisHashtags.java`)
- **Sorted sets** for priority queues
- **Pub/Sub** for operation state change notifications
- **Hashes** for operation storage, action results, worker registry

### Protobuf Definitions

Located in `src/main/protobuf/build/buildfarm/v1test/`:
- `buildfarm.proto` — Custom services (`Admin`, `OperationQueue`, `WorkerProfile`, `WorkerControl`) and messages (`QueueEntry`, `ShardWorker`, `BackplaneStatus`, etc.)
- `lru.proto` — Persistent LRU tracking schema for CAS

Standard Remote Execution API v2 protos come from the `@remoteapis` external dependency.

### CAS Backends

- `MemoryCAS` — In-memory with LRU eviction (testing/small deployments)
- `DirectoryEntryCFC` — Default file-system CAS with hex-bucket sharding and persistent LRU DB
- `LegacyDirectoryCFC` — Legacy flat-file variant
- `GrpcCAS` — Delegates to a remote CAS via gRPC

Workers can chain storage (e.g., local file cache → remote gRPC CAS). Supports Zstd compression.

## Code Conventions

- **Java 21** language level and runtime
- **Lombok**: `@Data` for config POJOs, `@lombok.extern.java.Log` for loggers (Java Util Logging)
- **Logging**: Use `log.log(Level.INFO, "message")` pattern (JUL, not SLF4J)
- **No DI framework** — Direct instantiation with config singleton
- **Testing**: JUnit 4 with custom `AllTests` suite runner (scans for `*Test.java`), Mockito for mocking, Truth for assertions, Jimfs for in-memory filesystem tests, gRPC InProcess for service tests
- **Test tags**: `redis` for tests needing Redis, `integration` for end-to-end tests (both excluded from `bazel test //...` by default)
- **Dependencies**: Maven artifacts managed via `rules_jvm_external` in `MODULE.bazel` (bzlmod, no WORKSPACE)
