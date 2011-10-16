package com.ettrema.backup.engine;

import java.util.Date;

public class DateAndLong {

    private final Date date;
    private final Long l;

    public DateAndLong(Date date, Long l) {
        this.date = date;
        this.l = l;
    }

    public Date getDate() {
        return date;
    }

    public Long getLong() {
        return l;
    }
}
