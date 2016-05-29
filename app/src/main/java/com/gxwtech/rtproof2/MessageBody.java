package com.gxwtech.rtproof2;

/**
 * Created by geoff on 5/29/16.
 */
public interface MessageBody {
    int getLength();
    void init(byte[] rxData);
    byte[] getRxData();
    void setRxData(byte[] rxData);
    byte[] getTxData();
    void setTxData(byte[] txData);
}
