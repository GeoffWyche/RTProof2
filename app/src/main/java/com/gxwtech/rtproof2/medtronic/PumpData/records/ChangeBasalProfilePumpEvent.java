package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class ChangeBasalProfilePumpEvent extends TimeStampedRecord {
    public ChangeBasalProfilePumpEvent() {
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(152,data,2);
    }
}
