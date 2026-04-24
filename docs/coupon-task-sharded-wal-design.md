# Design: Coupon Distribution with Sharded Parallelism + WAL Durability

## 1. Background and Goals

The current distribution flow has already moved from Excel to DB paging, but still has key limitations:

- Limited per-task throughput due to single-cursor processing.
- Progress durability is not strong enough for crash recovery if state is mostly in Redis.
- Event emission and checkpoint progression can still expose duplicate/missing windows.

This design introduces sharded parallel execution with WAL-style durable checkpoints to achieve:

- Higher throughput through shard-level parallelism.
- Reliable crash recovery and shard takeover.
- Stronger consistency and observability across execution, retries, and finalization.

## 2. Scope and Non-Goals

### 2.1 In Scope

- Sharded task execution model (`task -> shards`).
- Distributed shard lease and takeover.
- Durable shard-level WAL/checkpoint model.
- Exactly-once-like finalization (`distributionEndFlag=true` emitted once).
- End-to-end observability plan: tracing, metrics, and logs.

### 2.2 Out of Scope

- Replacing RocketMQ with Kafka in this phase.
- Introducing workflow engines (Temporal, etc.).
- Business rule changes (inventory semantics, eligibility semantics, failure taxonomies).

## 3. Target Architecture

```text
CouponTaskExecuteEvent
  -> CouponTaskExecuteConsumer (orchestrator)
      -> ShardScheduler (create/recover N shards)
          -> WorkerPool
              -> LeaseCoordinator (Redis)
              -> UserShardScanner (DB keyset paging)
              -> LuaPreDeduct + BatchEventEmitter
              -> ShardWALRepository (DB)
      -> FinalizeCoordinator (once-only)
```

## 4. Sharded Parallel Execution Model

### 4.1 Shard Strategy

Use user-id hash sharding:

- `shardId = hash(userId) % shardTotal`
- Example SQL:
    - `WHERE shop_number=? AND id>? AND MOD(id, shardTotal)=shardId ORDER BY id LIMIT pageSize`

Benefits:

- Deterministic single-shard ownership per user.
- Natural parallelism.
- Efficient crash recovery via shard-local cursor.

### 4.2 Execution Concurrency

- In each instance, `worker-thread-pool-size` workers process multiple shards in parallel.
- Across instances, workers compete for shard leases.
- `CouponTaskExecuteConsumer` acts as orchestrator, not full executor.

## 5. Distributed Lease and Takeover

### 5.1 Redis Lease Key

- `dist:task:{taskId}:shard:{shardId}:lease`
- value: `instanceId:workerId:expireAt`
- acquire: `SET NX EX leaseSeconds`
- renew: heartbeat with TTL refresh

### 5.2 Takeover Rules

- If lease expires (crash/network split), another worker can acquire it.
- New lease owner resumes from WAL checkpoint (`lastUserId`).
- At most one active owner per shard at any moment.

## 6. Durable WAL Model

## 6.1 Table Suggestion: `t_coupon_task_shard_wal`

Recommended columns:

- `id` bigint
- `task_id` bigint
- `task_batch_id` bigint
- `shard_id` int
- `status` varchar(16): `INIT` / `RUNNING` / `DONE` / `FAILED`
- `last_user_id` bigint
- `processed_count` bigint
- `batch_seq` bigint
- `lease_owner` varchar(128)
- `lease_expire_at` datetime
- `version` bigint (optimistic lock)
- `create_time` datetime
- `update_time` datetime
- unique key: `(task_id, shard_id)`

### 6.2 WAL Write Points

- Initialize shard row as `INIT`.
- After lease acquisition, mark `RUNNING`.
- After each page commit, persist `last_user_id` + `processed_count`.
- On shard completion, mark `DONE`.

### 6.3 Crash Recovery

On startup/reconciliation:

- Select `status in (INIT, RUNNING)` where lease expired.
- Re-assign shard and resume from `last_user_id`.
- Skip already `DONE` shards.

## 7. Batch Emission and Idempotency

### 7.1 Batch Triggering

- Keep current behavior: emit periodic events every `batchThreshold` (default 5000), `distributionEndFlag=false`.
- Emit one final event `distributionEndFlag=true` only after all shards are done.

### 7.2 Idempotency Keys

- periodic: `task:{taskId}:shard:{shardId}:batch:{batchSeq}`
- finalize: `task:{taskId}:finalize`

Downstream can keep existing dedup logic and add key-based suppression.

## 8. Finalize Once-Only Strategy

### 8.1 Completion Check

- finalize condition: `completedShards == shardTotal`
- source of truth: WAL aggregation (primary), optional Redis counter (acceleration)

### 8.2 Finalize CAS Lock

- lock key: `dist:task:{taskId}:finalize:once`
- only lock owner emits final message
- persist `finalized=true` in durable store to avoid replay duplicates

## 9. Configuration Recommendations

- `distribution.task.shard-total`: default 16
- `distribution.task.page-size`: default 500
- `distribution.task.batch-threshold`: default 5000
- `distribution.task.lease-seconds`: default 45
- `distribution.task.heartbeat-seconds`: default 15
- `distribution.task.worker-thread-pool-size`: default `min(shardTotal, cpu*2)`

## 10. Observability: Tracing + Metrics + Logging

This section defines core instrumentation points and operational signals.

### 10.1 Distributed Tracing (OpenTelemetry)

Use W3C Trace Context and propagate trace headers through MQ message metadata.

Core spans (recommended names):

- `coupon.task.consume` (entry span in `CouponTaskExecuteConsumer`)
- `coupon.task.shard.schedule`
- `coupon.task.shard.lease.acquire`
- `coupon.task.shard.lease.heartbeat`
- `coupon.task.shard.page.query`
- `coupon.task.user.lua.pre_deduct`
- `coupon.task.batch.emit`
- `coupon.task.wal.checkpoint.write`
- `coupon.task.finalize.check`
- `coupon.task.finalize.emit`

Required span attributes:

- `coupon.task.id`
- `coupon.task.batch_id`
- `coupon.shard.id`
- `coupon.batch.seq`
- `coupon.user.cursor`
- `coupon.page.size`
- `coupon.instance.id`
- `coupon.worker.id`
- `coupon.event.type` (`periodic` / `finalize`)
- `messaging.system` (`rocketmq`)
- `messaging.destination`

Error semantics:

- Set span status to `ERROR` on lease failures, DB checkpoint failures, Lua failures, or MQ send failures.
- Add exception type/message and retry attempt as attributes.

### 10.2 Metrics (Prometheus-style)

Counters:

- `coupon_task_shard_lease_acquire_total{result}`
- `coupon_task_shard_takeover_total`
- `coupon_task_page_query_total{result}`
- `coupon_task_lua_exec_total{result}`
- `coupon_task_batch_emit_total{event_type,result}`
- `coupon_task_wal_write_total{result}`
- `coupon_task_finalize_total{result}`

Histograms:

- `coupon_task_page_query_latency_ms`
- `coupon_task_lua_latency_ms`
- `coupon_task_wal_write_latency_ms`
- `coupon_task_batch_emit_latency_ms`
- `coupon_task_recovery_lag_seconds`

Gauges:

- `coupon_task_active_shards`
- `coupon_task_shard_lag_users`
- `coupon_task_checkpoint_staleness_seconds`
- `coupon_task_inflight_workers`
- `coupon_task_progress_ratio`

Suggested SLO-aligned alerts:

- Lease churn too high (`takeover_total` spikes).
- Checkpoint staleness above threshold.
- Finalize not emitted within timeout after all shards done.
- WAL write failure ratio above threshold.
- Batch emit error ratio above threshold.

### 10.3 Structured Logging

Use structured JSON logs and enforce correlation fields.

Mandatory log fields:

- `trace_id`, `span_id`
- `task_id`, `task_batch_id`, `shard_id`
- `instance_id`, `worker_id`
- `cursor`, `page_size`, `processed_count`
- `batch_seq`, `event_type`, `distribution_end_flag`
- `lease_owner`, `lease_expire_at`
- `retry_count`, `error_code`, `error_message`

Log levels:

- `INFO`: shard start/end, periodic checkpoints, batch emission success.
- `WARN`: lease contention, duplicate suppression, transient retries.
- `ERROR`: checkpoint persist failure, irreversible MQ publish failure, finalize conflict after retries.

Sampling recommendation:

- Keep full logs for `ERROR`.
- Sample high-frequency `INFO` page logs (for example 1/N pages) while preserving periodic checkpoint logs.

## 11. Failure Scenarios and Recovery

### Scenario A: Worker crashes during shard processing

- Lease expires; another worker takes over.
- Resume from durable `last_user_id`.

### Scenario B: Crash near event emission

- Risk window around event send/checkpoint update.
- Mitigation path:
    - Current phase: idempotency key + replay-safe downstream handling.
    - Next phase: Outbox + relay for stronger atomicity.

### Scenario C: Redis instability

- Lease acquire/heartbeat failures block ownership progression.
- Workers should pause shard progression and retry with backoff.

## 12. Rollout Plan

### Phase 1 (Minimum viable durable parallelism)

- Introduce shard scheduler, lease coordinator, shard WAL.
- Refactor consumer into orchestrator + shard workers.

### Phase 2 (Idempotency hardening)

- Add shard-batch sequence idempotency keys.
- Add durable `finalized` marker and startup reconciliation.

### Phase 3 (Durability hardening)

- Add Outbox + async relay for state/event consistency.
- Add automated recovery jobs and replay tools.

## 13. Risks and Mitigations

- **Shard skew / long tail**
    - Increase shard count, support dynamic shard stealing.
- **WAL write amplification**
    - Checkpoint by page, not by user.
- **Redis dependency risk**
    - Keep Redis as lease coordinator only; DB WAL is recovery source of truth.
- **Duplicate event risk**
    - Idempotency keys + finalize CAS + downstream dedup.

## 14. Acceptance Criteria

- Multi-instance execution improves throughput for one task.
- Crash/restart can recover from last durable checkpoint without broad reprocessing.
- Final event (`distributionEndFlag=true`) is emitted once per task.
- Traces, metrics, and logs provide actionable visibility for shard progress, takeover, and event health.

## 15. Revision History

| Version | Date       | Notes                                                                     |
|---------|------------|---------------------------------------------------------------------------|
| 1.0     | 2026-04-23 | Initial English version with tracing/metrics/logging observability design |
