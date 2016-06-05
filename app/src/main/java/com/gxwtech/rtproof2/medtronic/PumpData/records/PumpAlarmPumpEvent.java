package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class PumpAlarmPumpEvent extends TimeStampedRecord {
    private int rawtype = 0;
    public PumpAlarmPumpEvent() {
    }
    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (!simpleParse(9,data,4)) {
            return false;
        }
        rawtype = asUINT8(data[1]);
        return true;
    }

}
