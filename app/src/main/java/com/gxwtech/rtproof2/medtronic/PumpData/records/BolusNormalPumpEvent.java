package com.gxwtech.rtproof2.medtronic.PumpData.records;


import android.util.Log;

import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;


public class BolusNormalPumpEvent extends TimeStampedRecord {
    private final static String TAG = "BolusNormalPumpEvent";

    private double programmedAmount = 0.0;
    private double deliveredAmount = 0.0;
    private int duration = 0;
    private double unabsorbedInsulinTotal = 0.0;
    private String bolusType = "Unset";

    public BolusNormalPumpEvent() {
    }

    private double insulinDecode(int a, int b) {
        return ((a << 8) + b) / 40.0;
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (PumpModel.isLargerFormat(model)) {
            length = 13;
        } else {
            length = 9;
        }
        if (length > data.length) {
            return false;
        }
        if (PumpModel.isLargerFormat(model)) {
            programmedAmount = insulinDecode(asUINT8(data[1]),asUINT8(data[2]));
            deliveredAmount = insulinDecode(asUINT8(data[3]),asUINT8(data[4]));
            unabsorbedInsulinTotal = insulinDecode(asUINT8(data[5]),asUINT8(data[6]));
            duration = asUINT8(data[7]) * 30;
            timestamp = new PumpTimeStamp(TimeFormat.parse5ByteDate(data,8));
        } else {
            programmedAmount = asUINT8(data[1]) / 10.0f;
            deliveredAmount = asUINT8(data[2]) / 10.0f;
            duration = asUINT8(data[3]) * 30;
            unabsorbedInsulinTotal = 0;
            timestamp = new PumpTimeStamp(TimeFormat.parse5ByteDate(data,4));
        }

        bolusType = (duration > 0) ? "square" : "normal";

        return true;
    }

}
