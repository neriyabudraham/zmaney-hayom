package com.botomat.zmaneyhayom.models;

public enum OffsetType {
    AT_TIME("at_time", "בזמן"),
    MINUTES_BEFORE("minutes_before", "דקות לפני"),
    MINUTES_AFTER("minutes_after", "דקות אחרי");

    private final String value;
    private final String hebrewName;

    OffsetType(String value, String hebrewName) {
        this.value = value;
        this.hebrewName = hebrewName;
    }

    public String getValue() { return value; }
    public String getHebrewName() { return hebrewName; }

    public static OffsetType fromString(String value) {
        for (OffsetType type : values()) {
            if (type.value.equals(value)) return type;
        }
        return AT_TIME;
    }
}
