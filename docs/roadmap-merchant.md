# Merchant Refactor Roadmap

## Phase 0: Baseline Isolation (done in refactor workspace)

## Phase 1: Entity First

- Isolation merchant SQL: db/merchant-admin.sql
- Local infra compose: docker/docker-compose.yaml
- Scope limited to merchant domain (template + task)

## Phase 2: Controller and API Contract

- Coupon Task APIs
    - create task
    - page query task
    - task detail
- Coupon Template APIs
    - create template
    - page query template
    - template detail
    - increase stock
    - terminate template
- Maintain DTO compatibility unless explicitly introducing breaking changes

## Phase 3: Task Flow Stabilization (Excel -> Task -> MQ)

Primary goal: ensure the end-to-end path is reliable before optimization

- Ensure Excel-based uploads can consistently:
    - trigger task creation
    - complete task transformation
    - successfully dispatch to MQ
- Treat this as the critical path: `Excel -> Parsing -> Task -> MQ`
- Add comprehensive test coverage as safeguards:
    - file parsing correctness
    - task creation idempotency
    - MQ dispatch success/failure handling
- Establish baseline guarantees:
    - no task loss
    - clear failure visibility
    - retry-safe behavior

## Phase 4: Excel Processing Optimization

- Move heavy parsing and validation off the request hot path
- Introduce pre-validation with clear error feedback
- Adopt submit-and-return pattern for task creation
- Prepare for async processing scalability

## Phase 5: Task Orchestration Upgrade (Temporal)

After the core pipeline is stable, evolve scheduling/orchestration layer

- Replace in-app task scheduler with Temporal-based orchestration
- Improvements introduced
- Refactor integration with App Service:
    - clearly define activity boundaries
    - decouple business logic from orchestration
    - standardize interfaces between Temporal workflows and services

---

# Key Evolution Strategy

First: correctness

- Ensure Excel -> Task -> MQ path is reliable

Then: safety

- Add test coverage and failure handling

Finally: architecture upgrade

- Introduce Temporal for orchestration nd observability 