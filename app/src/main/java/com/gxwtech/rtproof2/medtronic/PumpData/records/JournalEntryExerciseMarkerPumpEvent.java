package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class JournalEntryExerciseMarkerPumpEvent extends TimeStampedRecord {
    public JournalEntryExerciseMarkerPumpEvent(){}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(8, data, 2);
    }
}
