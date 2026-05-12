package com.blueWhale.Rahwan.order;

/**
 * Distinguishes regular delivery orders from charity donation orders (WasalElkheer).
 *
 * REGULAR : standard paid delivery — user pays, app takes commission, driver earns net.
 * CHARITY : donation delivery   — app pays driver in full, no commission, no user wallet charge.
 */
public enum OrderCategory {
    REGULAR,
    CHARITY
}