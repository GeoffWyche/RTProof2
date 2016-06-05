package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class ChangeTempBasalTypePumpEvent extends TimeStampedRecord {
    private boolean isPercent=false; // either absolute or percent
    public ChangeTempBasalTypePumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (!simpleParse(7,data,2)) {
            return false;
        }
        if (asUINT8(data[1])==1) {
            isPercent = true;
        } else {
            isPercent = false;
        }
        return true;
    }
}
