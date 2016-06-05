package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class TempBasalDurationPumpEvent extends TimeStampedRecord {
    private int durationMinutes = 0;
    public TempBasalDurationPumpEvent() { }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        if (!simpleParse(7,data,2)) {
            return false;
        }
        durationMinutes = asUINT8(data[1]) * 30;
        return true;
    }

}
