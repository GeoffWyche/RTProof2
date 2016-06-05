package com.gxwtech.rtproof2.medtronic.PumpData.records;

import com.gxwtech.rtproof2.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class ChangeSensorRateOfChangeAlertSetupPumpEvent extends TimeStampedRecord {
    public ChangeSensorRateOfChangeAlertSetupPumpEvent() {}

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(12,data,2);
    }
}
