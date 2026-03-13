# Logging Strategy: Log Tables vs Loki

> Whether log data should live in RDBMS; how existing log tables relate to business/APIs; when to use Loki instead; and
> how to separate “business audit” from “observability logs”.

---

## 1. Current Log Tables and Business/API Usage

### 1.1 Template operation log: `t_coupon_template_log` / `t_coupon_template_log_*`

- **Entity**: `CouponTemplateLogDO` (merchant-admin)
- **Write**: `DBLogRecordServiceImpl` implements BizLog’s `ILogRecordService`; when
  `logRecord.getType() == "CouponTemplate"`, it inserts `shop_number`, `coupon_template_id`, `operator_id`,
  `operation_log`, `original_data`, `modified_data` into the table.
- **Read**: `queryLog` / `queryLogByBizNo` currently return empty lists; **no query API is exposed yet**, so this is
  effectively one-way audit storage.

**Conclusion**: It is business-related—it records who changed which template when and how (operation audit), but it is
not yet used by any product feature (e.g. admin “operation history” page).

### 1.2 User coupon operation log: `t_user_coupon_log_*`

- Referenced only in `onecoupon.sql` and `UserCouponLogShardingTests`; **no DO/Mapper/Service read or write found** in
  the codebase—can be treated as **not in use**.

---

## 2. RDBMS for Logs vs Loki: When to Use Which

### 2.1 Good fit for Loki / log stack

- **Pure observability**: Exceptions, debug output, QPS/latency, trace logs.
- **No need for structured query inside the application**; only ops/troubleshooting/analytics via labels and full-text
  search.
- **Benefits**: Easy to scale, flexible querying, lower cost; write amplification acceptable; read load handled by
  Loki/Promtail/Tempo.

### 2.2 Good fit for RDBMS (or OLAP)

- **Business audit / change history** that is not “text logs to read”:
    - e.g. Which merchant changed which template’s stock, valid_end_time, receive_rule, when; must be queryable in admin
      UI, exportable, and auditable for compliance.
- This is closer to **history/audit tables or event-sourcing snapshots** and should be treated as business data, not
  “application logs”.

---

## 3. Can Loki Replace Current Log Tables?

**Conclusion**: If there is **no** hard requirement to “query operation history by merchant/template/operator inside the
system”, you can stop expanding DB log tables and use **Loki with structured logs** for observability. If there **is**
such a product/compliance need, treat “audit” as a proper business store (RDBMS/OLAP) and keep it separate from
“observability logs” (Loki).

### 3.1 Why “logs in RDBMS” is often a bad fit

- **Performance and cost**: Under high QPS, writing operation logs to MySQL/sharded tables competes with core
  transaction tables; sharding, scaling, and archival add complexity.
- **Observability**: Searching by traceId, service, time, etc. is less flexible than Loki + Grafana; cross-instance,
  cross-service query experience is poor.

### 3.2 Recommended layering

1. **System logs → Loki (recommended)**
    - Use logback/log4j2 with Loki appender; attach `traceId`, `userId`, `shopNumber`, `couponTemplateId` as labels or
      fields; ops and devs use Loki/Grafana for search and alerts.

2. **What to do with current DB operation logs**
    - **`t_user_coupon_log_*`**: No API usage today; do not enable or write. If user-coupon audit is needed later,
      decide between a dedicated audit table or outputting audit as structured logs to Loki/ES.
    - **`t_coupon_template_log`**:
        - If there is **no** strong need for “admin views template operation history” or “compliance traceability”: Keep
          BizLog but change `ILogRecordService` to **write to Loki** (or keep a small audit table with minimal fields);
          stop inserting into the current sharded log table.
        - If there **is** such a need: Treat this as a **TemplateAudit** business table, distinct from “observability
          logs”; optionally also emit the same change info as structured logs to Loki for troubleshooting.

3. **Future design**
    - In a CQRS/Saga design, domain events (e.g. TemplateCreated, CouponLocked) can be written to event/audit tables (
      business) and also emitted as structured logs to Loki; avoid ambiguous “is this log table core business?”
      situations.

---

## 4. Practical Recommendations

- **Short term**: Do not add write logic for `t_user_coupon_log_*`; leave `t_coupon_template_log` as-is, and after Loki
  is in place, log key fields and errors from `DBLogRecordServiceImpl` for troubleshooting; later decide whether to
  retire the table based on access patterns.
- **Medium term (cloud-native refactor)**: Design **Audit** as its own domain—audit that must be queryable in the
  product goes to a dedicated audit store (or ClickHouse/ES); everything else for debugging and ops goes to Loki; remove
  “log” tables that exist only for recording and blur the line with business data.

**Summary**: Using RDBMS for pure observability logs is indeed unstable and costly; if a “log table” is really business
audit, define it clearly and use Loki for observability and a dedicated audit store for business queries and compliance.
