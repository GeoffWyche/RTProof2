package com.gxwtech.rtproof2.Messages;

import com.gxwtech.rtproof2.MessageBody;

/**
 * Created by geoff on 5/29/16.
 */
public class GenericMessageBody implements MessageBody {
    byte[] body = new byte[0];
    @Override
    public int getLength() {
        return body.length;
    }

    public GenericMessageBody(byte[] data) {
        init(data);
    }

    @Override
    public void init(byte[] rxData) {
        body = rxData;
    }

    @Override
    public byte[] getRxData() {
        return body;
    }

    @Override
    public void setRxData(byte[] rxData) {
        init(rxData);
    }

    @Override
    public byte[] getTxData() {
        return body;
    }

    @Override
    public void setTxData(byte[] txData) {
        init(txData);
    }
}
