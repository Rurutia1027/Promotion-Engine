# Merchant Refactor Roadmap

## Phase 0: Baseline Isolation (done in refactor workspace)

## Phase 1: Entity First

- Isolated merchant SQL: `db/merchant-admin.sql`
- Local infra compose: `docker/docker-compose.yaml`
- Kept scope to merchant only (template + task domain)

## Phase 2: Controller and API Contract

- Coupon task APIs
    - create task
    - page query task
    - task detail
- Coupon template APIs
    - create template
    - page query template
    - template detail
    - increase stock
    - terminate template
- Keep DTO compatibility unless explicitly breaking.

## Phase 3: Task Flow Optimization

- Optimize task status transitions and idempotency
- Keep MQ handoff contract stable for downstream distribution
- Add clear retry/error classification for task send path.

## Phase 4: Excel Optimization (merchant side)

- Move heavy parsing/validation out of request hot path.
- Add pre-validation and clear error feedback for file problems.
- Keep task creation endpoint lightweight (submit-and-return pattern).