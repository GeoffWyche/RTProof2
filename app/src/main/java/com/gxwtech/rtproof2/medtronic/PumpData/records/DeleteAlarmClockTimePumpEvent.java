package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class DeleteAlarmClockTimePumpEvent extends TimeStampedRecord {
    public DeleteAlarmClockTimePumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(14,data,2);
    }
}
