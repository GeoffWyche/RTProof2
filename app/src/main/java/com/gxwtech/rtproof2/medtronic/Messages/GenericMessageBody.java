package com.gxwtech.rtproof2.medtronic.Messages;

/**
 * Created by geoff on 5/29/16.
 */
public class GenericMessageBody extends MessageBody {
    byte[] body = new byte[0];

    public int getLength() {
        return body.length;
    }

    public GenericMessageBody(byte[] data) {
        init(data);
    }

    public void init(byte[] rxData) {
        body = rxData;
    }

    public byte[] getRxData() {
        return body;
    }

    public void setRxData(byte[] rxData) {
        init(rxData);
    }

    public byte[] getTxData() {
        return body;
    }

    public void setTxData(byte[] txData) {
        init(txData);
    }
}
