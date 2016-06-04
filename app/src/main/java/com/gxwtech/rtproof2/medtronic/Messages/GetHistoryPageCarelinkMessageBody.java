package com.gxwtech.rtproof2.medtronic.Messages;

import com.gxwtech.rtproof2.util.ByteUtil;

/**
 * Created by geoff on 6/2/16.
 */
public class GetHistoryPageCarelinkMessageBody extends CarelinkLongMessageBody {
    public boolean wasLastFrame = false;
    public int frameNumber = 0;
    public byte[] frame = new byte[] {};

    public GetHistoryPageCarelinkMessageBody(int pageNum) {
        init(pageNum);
    }

    public int getLength() {
        return frame.length + 1;
    }

    public void init(byte[] rxData) {
        super.init(rxData);
        if (rxData.length > 0) {
            frameNumber = rxData[0] & 0x7f;
            wasLastFrame = (rxData[0] & 0x80) != 0;
            if (rxData.length > 1) {
                frame = ByteUtil.substring(rxData, 1, rxData.length-1);
            }
        }

    }

    public void init(int pageNum) {
        byte numArgs = 1;
        super.init(new byte[] {numArgs,(byte)pageNum});
    }

}
