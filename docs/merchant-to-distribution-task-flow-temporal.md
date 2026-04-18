# Merchant -> Distribution Single-Flow Runbook and Temporal Upgrade Plan

> This document focuses on one flow only: `merchant-service` (currently implemented in the `merchant-admin` module)
> publishes task events execution, and progressively upgrades this flow to Temporal orchestration.

---

## 1. Goals and scope

This focused document covers three outcomes:

- **Observable flow**: complete critical logs/metrics for `merchant -> RocketMQ -> distribution -> Excel execution`.
- **Runnable flow**: provide executable validation steps to prove publish/consume/Excel execution end-to-end.
- **Upgradeable flow**: move this execution path to Temporal workflow/activity incrementally, without a full rewrite.

Out of scope:

- global refactor of all topics;
- immediate full migration to fat messages everywhere;
- full-domain Saga orchestration

---

## 2. Current implementation flow (as-is)

### 2.1 Producer side (merchant)

- Entry point: `CouponTaskJobHandler#distributeCoupon`
    - update task state to `IN_PROGRESS`;
    - build `CouponTaskExecuteEvent(couponTaskId)`
    - publish using `CouponTaskActualExecuteProducer#sendMessage`.
- Producer class: `CouponTaskActualExecuteProducer`
    - message key = `couponTaskId`
    - topic = `TEMPLATE_TASK_EXECUTE_TOPIC_KEY`
    - payload = `MessageWrapper(keys, CouponTaskExecuteEvent)`.

### 2.2 Consumer side (distribution)

- Entry point: `CouponTaskExecuteConsumer#onMessage`
    - uses `@NoMQDuplicationConsume` with `couponTaskId` as idempotent key (120s);
    - fetch tasks by ID (`couponTaskMapper#selectById`)
    - validates task status `IN_PROGRESS`
    - validates template status `ACTIVE`
    - creates `ReadExcelDistributionListener` and runs `EasyExcel#read(...).doRead()`

### 2.3 Excel execution and downstream dispatch

- `ReadExcelDistributionListener#invoke`
    - skips already-processed rows via Redis progress key;
    - runs Lua for stock decrement + user set staging;
    - writes failures to `t_coupon_task_fail` when stock is insufficient;
    - publishes `CouponTemplateDistributionEvent` when conditions are met.
- `ReadExcelDistributionListener#doAfterAllAnalysed`
    - emits `distributionEndFlag=true` completion event for downstream finalization.

---

## 3. Logging and metrics design for this flow

### 3.1 Required correlation fields

Use consistent structured fields (MDC or JSON log fields) across all services:

- `traceId`
- `eventId`
- `couponTaskId`
- `mqTopic`
- `mqKeys`
- `shopNumber`
- `couponTemplateId`
- `batchId`
- `phase` (`publish` / `consume` / `excel_invoke` / `excel_finish`)

### 3.2 Minimal critical log points

1. **After merchant task claim**

- `task_claimed`: state transition result (`PENDING -> IN_PROGRESS`, affected rows).

2. **Before/after merchant publish**

- `mq_publish_start`: topic, key, eventId
- `mq_publish_success | failed`: msgId, latency, exception

3. **On distribution consume**

- `mq_consume_start`: topic, key, eventId, couponTaskId;
- `mq_consume_skip`: reason (status mismatch, idempotency conflict, etc.).

4. **During Excel processing**

- `excel_row_processed` (sampled)
- `excel_row_failed` (stock / data reason)
- `excel_distribution_event_sent` (batch size, end flag)

5. **At Excel completion**

- `excel_execute_finished`: total rows, failed rows, duration.

### 3.3 Suggested metrics (Prometheus/Micrometer)

- `coupon_task_publish_total{result}`
- `coupon_task_publish_latency_ms`
- `coupon_task_consume_total{result}`
- `coupon_task_consume_latency_ms`
- `coupon_task_excel_rows_total{result}`
- `coupon_task_excel_duration_ms`
- `coupon_task_fail_rows_total{cause}`

### 3.4 OpenTelemetry tracing across message boundaries

Metrics + logs are necessary but not sufficient for async flows. We need tracing to debug cross-service message paths
efficiently.

Recommended baseline:

- **Unified trace context propagation**
    - inject `traceparent` / `tracestate` into MQ headers on publish;
    - extract context on consume and create a `CONSUMER` span;
    - create child spans for critical Excel stage (for example `excel.row.batch.pocess`).
- **Span naming convention**
    - `mq.publish.coupon_task_execute`
    - `mq.consume.coupon_task_execute`
    - `excel.execute.coupon_task`
    - `mq.publish.coupon_template_distribution`
- **Minimum span attributes**
    - `messaging.system=rocketmq`
    - `message.destination` (topic)
    - `messaging.message_id` (if available)
    - `messaging.operation` (publish/receive/process)
    - `coupon.task.id`, `coupon.template.id`, `shop.number`
- **Logs aligned with traces**
    - output `traceId` and `spanId` in structured logs;
    - make alerting dashboards jum from metric -> trace -> logs.

---

## 4. End-to-end validation checklist (single flow)

### 4.1 Preconditions

- `merchant-service`, `distribution`, `redis`, and `rocketmq` are up;
- one executable task exists and `fileAddress` is accessible;
- template status is `ACTIVE`

### 4.2 Validation steps

- Trigger task scheduling form merchant side (or manually trigger the flow invoking `distributeCoupon`)
- Verify merchant logs include `mq_publish_success`
- Verify distribution logs include `mq_consume_start`
- Verify Excel row-processing logs and Redis progress updates.
- Verify `CouponTemplateDistributionEvent` publish logs and final `distributionEndFlag=true` event.
- Spot-check data consistency: stock, failure rows, and downstream dispatch outputs.

### 4.3 Definition of Done (DoD)

- Full path from task trigger to Excel completion runs without interruption;
- a single `couponTaskId` can be traced end-to-end across producer/consumer/excel logs;
- failure paths (stock shortage, duplicate message) are visible and explainable.
- a single `traceId` is continuous across `merchant -> MQ -> distribution -> excel`.

### 4.4 Testcontainers-based automated E2E verification

Turn this flow into CI-ready integration coverage:

1. **Container dependencies**

- RocketMQ (nameserver + broker)
- Redis
- MySQL (for your current test DB)
- Otel collector (for trace export assertations)

2. **Test input setup**

- seed one executable `PENDING` task and one `ACTIVE` template;
- generate a minimal Excel file (for example 10 rows) and make `fileAddress` accessible.

3. **Test actions**

- trigger merchant scheduling/publishing;
- wait for distribution consume and Excel execution completion;
- assert DB/Redis/MQ side effects

4. **Key assertions**

- task state transitions are correct;
- `t_coupon_task_fail` content matches scenario expectations;
- at least one `CouponTemplateDistributionEvent` is published;
- traces contain publish/consume/excel core spans.

5. **Failure artifacts for triage**

- automatically capture container logs, application logs, and trace export snapshots.

---

## 5. Known risks in current implementation

1. **Non-atomic status update + MQ publish**: task can be stuck in `IN_PROGRESS` if publish fails.
2. **Concurrent claiming race**: multiple schedulers may claim the same task.
3. **Null-safety gap**: `selectById` null path lead to NPE
4. **Limited recovery for long-running jobs**: large Excel processing depends on Redis progress but lacks unified
   orchestration/retry semantics.

---

## 6. Upgrading this flow to Temporal (to-be)

### 6.1 Design objective

- Convert "schedule + publish + consume + Excel execute + finalize" into an observable, retryable, recoverable workflow;
- keep existing business operations as activities to avoid big-bang rewrite.

### 6.2 Recommended workflow decomposition

`CouponTaskExecuteWorkflow(couponTaskId)`

- ClaimTaskActivity: CAS-based task claiming (with version/lease).
- PublishTaskEventActivity: Outbox write or MQ publish.
- WaitConsumeAckActivity: wait for consumption acknowledgement/progress signal.
- ExecuteExcelActivity: chunk/row-batch execution.
- FinalizeTaskActivity: persist final state/statistics and emit completion event.

### 6.3 Relationship with existing MQ

- **Stage 1**: Temporal wraps existing MQ path first (MQ remains online)
- **Stage 2**: move retry/timeout ownership from listener code into Temporal retry policies.
- **Stage 3**: selectively replace some MQ-internal transitions with direct activities, while MQ remains cross-domain
  event bus.

---

---

## 7. Migration milestones (recommended 4 phases)

### M1: Observability first

- add complete structured logs and metrics;
- enable task-level traceability;
- add alerts for publish/consume/excel failures.

### M2: Reliability hardening

- switch task claiming to CAS;
- introduce Outbox or compensation retry jobs;
- strengthen null/status-guard handling.

### M3: Temporal wrapper

- introduce `CouponTaskExecuteWorkflow` to orchestrate current steps;
- hand over retry/timeouts to Temporal policies;
- log `workflowId`/`runId` at key checkpoints.

### M4: Temporal deepening

- split Excel execution into chunked activities;
- provide replay and operator intervention entry points;
- finalize runbook for retry/terminate/compensate/rollback.

---

## 8. Rollback and degradation strategy for this flow

1. **Flow rollback**: keep existing scheduler + MQ path, enable Temporal path by gradual rollout.
2. **Code rollback**: activities call existing services, preserving business semantics; instant fallback to old path
   remains available.
3. **Data rollback**: Temporal metadata remains isolated from business tables; rollback stops workflows without mutating
   business schema.
4. **Traffic rollback**: canary by `couponTaskId` ranges; switch back immediately when error thresholds are exceeded.

---

## 9. Minimum implementation backlog (start here)

- add unified structured logging fields in `CouponTaskActualExecuteProducer` and `CouponTaskExecuteConsumer`;
- add Excel start/end/failure stats logs;
- integrate OTel propagation (header inject/extract + span attribute conventions);
- add one-task Testcontainers E2E scenario and acceptance checklist;
- define `CouponTaskExecuteWorkflow` interface and activity inventory (without replacing current business logic yet).



