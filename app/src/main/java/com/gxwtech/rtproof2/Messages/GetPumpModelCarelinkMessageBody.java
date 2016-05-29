package com.gxwtech.rtproof2.Messages;

import com.gxwtech.rtproof2.MessageBody;

/**
 * Created by geoff on 5/29/16.
 */
public class GetPumpModelCarelinkMessageBody implements MessageBody {
    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public void init(byte[] rxData) {

    }

    @Override
    public byte[] getRxData() {
        return new byte[] { 0 };
    }

    @Override
    public void setRxData(byte[] rxData) {

    }

    @Override
    public byte[] getTxData() {
        return new byte[] { 0 };
    }

    @Override
    public void setTxData(byte[] txData) {

    }
}
