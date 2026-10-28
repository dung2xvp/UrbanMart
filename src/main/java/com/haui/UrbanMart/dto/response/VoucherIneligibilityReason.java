package com.haui.UrbanMart.dto.response;

public enum VoucherIneligibilityReason {
    EMPTY_CART,
    BRANCH_MISMATCH,
    NOT_STARTED,
    EXPIRED,
    MIN_ORDER_VALUE,
    GLOBAL_USAGE_LIMIT_REACHED,
    USER_USAGE_LIMIT_REACHED,
    FIRST_ORDER_ONLY,
    SPECIAL_DAY_ONLY,
    NO_APPLICABLE_PRODUCTS
}
