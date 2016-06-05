package com.gxwtech.rtproof2.medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.medtronic.PumpTimeStamp;

import org.joda.time.DateTime;


abstract public class Record {
    private static final String TAG = "Record";
    protected byte recordOp;
    protected int length;

    protected String recordTypeName = this.getClass().getSimpleName();
    public String getRecordTypeName() {return recordTypeName;}

    public Record() {
        length = 1;
    }

    public boolean parseFrom(byte[] data, PumpModel model) {
        if (data == null) {
            return false;
        }
        if (data.length < 1) {
            return false;
        }
        recordOp = data[0];
        return true;
    }

    public PumpTimeStamp getTimestamp() {
        return new PumpTimeStamp();
    }

    public int getLength() { return length; }

    public byte getRecordOp() {
        return recordOp;
    }

    protected static int asUINT8(byte b) { return (b<0)?b+256:b;}
}
