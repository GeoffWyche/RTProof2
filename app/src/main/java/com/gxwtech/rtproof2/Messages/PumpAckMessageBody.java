package com.gxwtech.rtproof2.Messages;

import com.gxwtech.rtproof2.MessageBody;

/**
 * Created by geoff on 5/29/16.
 */
public class PumpAckMessageBody implements MessageBody {
    public static int length;
    public int getLength() { return 1; }

    private byte[] rxData;

    public PumpAckMessageBody(byte[] bodyData) {
        init(bodyData);
    }

    public void init(byte[] rxData) {
        this.rxData = rxData;
    }

    public byte[] getRxData() {
        return rxData;
    }
    public void setRxData(byte[] rxData) {
        this.rxData = rxData;
    }
    public byte[] getTxData() {
        return rxData;
    }
    public void setTxData(byte[] txData) {
        this.rxData = txData;
    }
}
