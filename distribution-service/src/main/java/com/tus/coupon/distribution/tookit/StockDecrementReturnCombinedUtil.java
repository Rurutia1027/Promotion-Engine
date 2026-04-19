package com.tus.coupon.distribution.tookit;

// redis return combined fields parser util class
public class StockDecrementReturnCombinedUtil {
    private static final int SECOND_FIELD_BITS = 13;

    /**
     * combined two fields into a combined value
     */
    public static int combineFields(boolean decrementFlag, int userRecord) {
        return (decrementFlag ? 1 : 0) << SECOND_FIELD_BITS | userRecord;
    }

    /**
     * extract first field value and return form the combined two fields value
     */
    public static boolean extractFirstField(long combined) {
        return (combined >> SECOND_FIELD_BITS) != 0;
    }

    /**
     * extract second field value and return from the combined two fields value
     */
    public static int extractSecondField(int combined) {
        return combined & ((1 << SECOND_FIELD_BITS) - 1);
    }
}
