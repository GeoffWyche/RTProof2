package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;

public class VariableSizeBodyRecord extends Record {

    public VariableSizeBodyRecord() {
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return true;
    }

    protected boolean decode(byte[] data) {
        return true;
    }
}
