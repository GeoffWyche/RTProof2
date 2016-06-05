package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

import javax.net.ssl.CertPathTrustManagerParameters;

public class CalBgForPhPumpEvent extends TimeStampedRecord {
    private int amount = 0;
    public CalBgForPhPumpEvent() {
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (!simpleParse(7,data,2)) {
            return false;
        }
        amount = ((asUINT8(data[6]) & 0x80) << 1) + asUINT8(data[1]);
        return true;
    }
}
