package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class DeleteBolusReminderTimePumpEvent extends TimeStampedRecord {
    public DeleteBolusReminderTimePumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(9,data,2);
    }
}
