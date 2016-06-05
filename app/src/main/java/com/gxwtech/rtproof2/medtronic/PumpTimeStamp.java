package com.gxwtech.rtproof2.medtronic;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Created by geoff on 6/4/16.
 * Exists to easily merge 2 byte timestamps and 5 byte timestamps.
 */
public class PumpTimeStamp {
    private LocalDateTime localDateTime = new LocalDateTime();
    public PumpTimeStamp() {}
    public PumpTimeStamp(LocalDate localDate) {
        localDateTime = new LocalDateTime(localDate);
    }
    public PumpTimeStamp(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }
    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }
    @Override
    public String toString() {
        return getLocalDateTime().toString();
    }
}
