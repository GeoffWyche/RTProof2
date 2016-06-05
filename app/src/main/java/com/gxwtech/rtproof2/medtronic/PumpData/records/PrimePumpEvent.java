package com.gxwtech.rtproof2.medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;


public class PrimePumpEvent extends TimeStampedRecord {
    private double amount=0.0;
    private double programmedAmount=0.0;
    private String primeType = "unknown";

    public PrimePumpEvent() {
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (!simpleParse(10,data,5)) {
            return false;
        }
        amount = (double)(asUINT8(data[4])<<2) / 40.0;
        programmedAmount = (double)(asUINT8(data[2])<<2) / 40.0;
        primeType = programmedAmount == 0 ? "manual" : "fixed";
        return true;
    }
}
