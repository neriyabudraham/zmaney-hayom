package com.botomat.zmaneyhayom.models;

public class AlertRule {
    private long id;
    private ZmanType zmanType;
    private OffsetType offsetType;
    private int offsetMinutes;
    private boolean enabled;
    private boolean soundEnabled;
    private boolean vibrateEnabled;

    public AlertRule() {
        this.enabled = true;
        this.soundEnabled = true;
        this.vibrateEnabled = true;
        this.offsetType = OffsetType.AT_TIME;
        this.offsetMinutes = 0;
    }

    public AlertRule(long id, ZmanType zmanType, OffsetType offsetType, int offsetMinutes,
                     boolean enabled, boolean soundEnabled, boolean vibrateEnabled) {
        this.id = id;
        this.zmanType = zmanType;
        this.offsetType = offsetType;
        this.offsetMinutes = offsetMinutes;
        this.enabled = enabled;
        this.soundEnabled = soundEnabled;
        this.vibrateEnabled = vibrateEnabled;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public ZmanType getZmanType() { return zmanType; }
    public void setZmanType(ZmanType zmanType) { this.zmanType = zmanType; }
    public OffsetType getOffsetType() { return offsetType; }
    public void setOffsetType(OffsetType offsetType) { this.offsetType = offsetType; }
    public int getOffsetMinutes() { return offsetMinutes; }
    public void setOffsetMinutes(int offsetMinutes) { this.offsetMinutes = offsetMinutes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
    public boolean isVibrateEnabled() { return vibrateEnabled; }
    public void setVibrateEnabled(boolean vibrateEnabled) { this.vibrateEnabled = vibrateEnabled; }

    public String getDisplayText() {
        String zmanName = zmanType.getHebrewName();
        switch (offsetType) {
            case MINUTES_BEFORE:
                return offsetMinutes + " דקות לפני " + zmanName;
            case MINUTES_AFTER:
                return offsetMinutes + " דקות אחרי " + zmanName;
            case AT_TIME:
            default:
                return "בזמן " + zmanName;
        }
    }
}
