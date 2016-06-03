package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;

public class UnabsorbedInsulin extends VariableSizeBodyRecord {

    public UnabsorbedInsulin() {
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        bodySize = readUnsignedByte(data[1]);
        calcSize();
        return true;
    }
}
