package com.bcb.enums;

public enum TimeInterval {
    ONE_MINUTE(60 * 1000),
    FIVE_MINUTES(5 * ONE_MINUTE.getMillis()),
    TEN_MINUTES(10 * ONE_MINUTE.getMillis()),
    ONE_HOUR(60 * ONE_MINUTE.getMillis()),
    ONE_DAY(24 * ONE_HOUR.getMillis());

    private final long millis;

    TimeInterval(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }
}
