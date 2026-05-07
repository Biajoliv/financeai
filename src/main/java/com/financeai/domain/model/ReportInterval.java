package com.financeai.domain.model;

public enum ReportInterval {
    WEEKLY(7),
    BIWEEKLY(15),
    MONTHLY(30);

    private final int days;

    ReportInterval(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}