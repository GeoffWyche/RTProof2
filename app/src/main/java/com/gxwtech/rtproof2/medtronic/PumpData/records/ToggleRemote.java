package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;

public class ToggleRemote extends TimeStampedRecord {

    public ToggleRemote() {
        bodySize = 14;
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}