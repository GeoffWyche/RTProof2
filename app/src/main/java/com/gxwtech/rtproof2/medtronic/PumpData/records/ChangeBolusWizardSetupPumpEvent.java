package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;
import com.gxwtech.rtproof2.medtronic.TimeFormat;

public class ChangeBolusWizardSetupPumpEvent extends TimeStampedRecord {

    public ChangeBolusWizardSetupPumpEvent() {

    }

    @Override
    public boolean parseFrom(byte[] data, PumpModel model) {
        return simpleParse(144,data,2);
    }
}
