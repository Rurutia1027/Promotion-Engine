# Merchant Refactor Scope

This folder is the isolated workspace for `merchant-admin` refactor.

## Current Business Scope

- Coupon task operations
    - create coupon push task
    - page query coupon push tasks
    - query coupon push task detail
- Coupon template operations
    - create coupon template
    - page query coupon templates
    - query coupon template detail
    - increase coupon stock
    - terminate coupon template

`search` is intentionally excluded for now.

## Refactor Order (agreed)

- DB isolation in refactor bundle
- Entity/model enhancement
- Controller completion and API alignment
- Task flow optimization
- Excel path optimization (last in merchant phase)