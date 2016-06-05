package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class ResultDailyTotalPumpEvent extends TimeStampedRecord {
    private static final String TAG = "ResultDailyTotalPumpEvent";
    public ResultDailyTotalPumpEvent() {
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (PumpModel.isLargerFormat(model)) {
            // record format changed with model number x23
            return simpleParse(10,data,5);
        } else {
            return simpleParse(7,data,5);
        }
    }
}
