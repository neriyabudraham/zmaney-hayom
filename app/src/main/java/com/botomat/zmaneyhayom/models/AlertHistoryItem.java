package com.botomat.zmaneyhayom.models;

import java.util.Date;

public class AlertHistoryItem {
    private long id;
    private ZmanType zmanType;
    private Date alertTime;
    private Date zmanTime;
    private OffsetType offsetType;
    private int offsetMinutes;
    private boolean dismissed;
    private boolean snoozed;

    public AlertHistoryItem() {}

    public AlertHistoryItem(ZmanType zmanType, Date alertTime, Date zmanTime,
                            OffsetType offsetType, int offsetMinutes) {
        this.zmanType = zmanType;
        this.alertTime = alertTime;
        this.zmanTime = zmanTime;
        this.offsetType = offsetType;
        this.offsetMinutes = offsetMinutes;
        this.dismissed = false;
        this.snoozed = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public ZmanType getZmanType() { return zmanType; }
    public void setZmanType(ZmanType zmanType) { this.zmanType = zmanType; }
    public Date getAlertTime() { return alertTime; }
    public void setAlertTime(Date alertTime) { this.alertTime = alertTime; }
    public Date getZmanTime() { return zmanTime; }
    public void setZmanTime(Date zmanTime) { this.zmanTime = zmanTime; }
    public OffsetType getOffsetType() { return offsetType; }
    public void setOffsetType(OffsetType offsetType) { this.offsetType = offsetType; }
    public int getOffsetMinutes() { return offsetMinutes; }
    public void setOffsetMinutes(int offsetMinutes) { this.offsetMinutes = offsetMinutes; }
    public boolean isDismissed() { return dismissed; }
    public void setDismissed(boolean dismissed) { this.dismissed = dismissed; }
    public boolean isSnoozed() { return snoozed; }
    public void setSnoozed(boolean snoozed) { this.snoozed = snoozed; }

    public String getDisplayDetails() {
        String zmanName = zmanType.getHebrewName();
        switch (offsetType) {
            case MINUTES_BEFORE:
                return offsetMinutes + " דקות לפני " + zmanName;
            case MINUTES_AFTER:
                return offsetMinutes + " דקות אחרי " + zmanName;
            default:
                return zmanName;
        }
    }
}
