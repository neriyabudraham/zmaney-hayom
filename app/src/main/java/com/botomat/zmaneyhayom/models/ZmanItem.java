package com.botomat.zmaneyhayom.models;

import java.util.Date;

public class ZmanItem {
    private ZmanType type;
    private String name;
    private Date time;
    private boolean passed;
    private boolean isNext;
    private boolean hasAlert;

    public ZmanItem(ZmanType type, String name, Date time) {
        this.type = type;
        this.name = name;
        this.time = time;
        this.passed = false;
        this.isNext = false;
        this.hasAlert = false;
    }

    public ZmanType getType() { return type; }
    public String getName() { return name; }
    public Date getTime() { return time; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public boolean isNext() { return isNext; }
    public void setNext(boolean next) { this.isNext = next; }
    public boolean hasAlert() { return hasAlert; }
    public void setHasAlert(boolean hasAlert) { this.hasAlert = hasAlert; }
}
