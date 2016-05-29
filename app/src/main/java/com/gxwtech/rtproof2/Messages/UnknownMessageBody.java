package com.gxwtech.rtproof2.Messages;

import com.gxwtech.rtproof2.MessageBody;

/**
 * Created by geoff on 5/29/16.
 */
public class UnknownMessageBody implements MessageBody {
    public byte[] rxData;

    @Override
    public int getLength() {
        return 0;
    }

    public UnknownMessageBody(byte[] data) {
        this.rxData = data;
    }

    @Override
    public void init(byte[] rxData) {
    }

    @Override
    public byte[] getRxData() {
        return rxData;
    }

    @Override
    public void setRxData(byte[] rxData) {
        this.rxData = rxData;
    }

    @Override
    public byte[] getTxData() {
        return rxData;
    }

    @Override
    public void setTxData(byte[] txData) {
        this.rxData = txData;
    }
}
