package com.gxwtech.rtproof2.medtronic.PumpData.records;


import com.gxwtech.rtproof2.medtronic.PumpModel;

public class Ian3F extends TimeStampedRecord {
  public Ian3F() {
    bodySize = 3;
    calcSize();
  }
  public boolean collectRawData(byte[] data, PumpModel model) {
    if (!super.collectRawData(data, model)) {
      return false;
    }
    return decode(data);
  }
}