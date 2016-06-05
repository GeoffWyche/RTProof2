package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class Sara6EPumpEvent extends TimeStampedRecord {
    public Sara6EPumpEvent() {
    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        length = 52;
        // We don't understand this event...
        // Minimum 16 characters? date components?
        if (16 > data.length) {
            return false;
        }
        timestamp = new PumpTimeStamp(TimeFormat.parse2ByteDate(data,1));
        return true;
    }

}
