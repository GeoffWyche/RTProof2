package com.gxwtech.rtproof2.medtronic.Messages;

/**
 * Created by geoff on 6/2/16.
 */
public class CarelinkLongMessageBody extends MessageBody {
    public static final int LONG_MESSAGE_BODY_LENGTH = 65;
    protected byte[] data;

    public CarelinkLongMessageBody() {
        data = new byte[LONG_MESSAGE_BODY_LENGTH];
    }

    public void init(byte[] rxData) {
        if (rxData != null) {
            int size = rxData.length < LONG_MESSAGE_BODY_LENGTH ? rxData.length : LONG_MESSAGE_BODY_LENGTH;
            for (int i=0; i<size; i++) {
                data[i] = rxData[i];
            }
        }
    }

    public int getLength() {
        return LONG_MESSAGE_BODY_LENGTH;
    }

    public byte[] getTxData() {
        return data;
    }

}
