package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class ChangeBasalProfilePatternPumpEvent extends TimeStampedRecord {
    public ChangeBasalProfilePatternPumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(152,data,2);
    }
}
