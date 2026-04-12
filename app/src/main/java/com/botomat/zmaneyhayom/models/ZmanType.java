package com.botomat.zmaneyhayom.models;

public enum ZmanType {
    ALOT("עלות השחר"),
    MISHEYAKIR("משיכיר"),
    HANETZ("הנץ החמה"),
    SOF_SHMA_MGA("סוף זמן ק״ש (מג״א)"),
    SOF_SHMA_GRA("סוף זמן ק״ש (גר״א)"),
    SOF_TFILA_MGA("סוף זמן תפילה (מג״א)"),
    SOF_TFILA_GRA("סוף זמן תפילה (גר״א)"),
    CHATZOT("חצות היום"),
    MINCHA_GEDOLA("מנחה גדולה"),
    MINCHA_KETANA("מנחה קטנה"),
    PLAG_MINCHA("פלג המנחה"),
    SHKIA("שקיעה"),
    TZEIT("צאת הכוכבים"),
    TZEIT_RT("צאת ר״ת"),
    CHATZOT_LAYLA("חצות הלילה");

    private final String hebrewName;

    ZmanType(String hebrewName) {
        this.hebrewName = hebrewName;
    }

    public String getHebrewName() {
        return hebrewName;
    }

    public static ZmanType fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return HANETZ;
        }
    }
}
