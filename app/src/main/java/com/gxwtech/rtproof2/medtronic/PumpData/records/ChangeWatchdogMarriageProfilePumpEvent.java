package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class ChangeWatchdogMarriageProfilePumpEvent extends TimeStampedRecord {
    public ChangeWatchdogMarriageProfilePumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(12,data,2);
    }
}
