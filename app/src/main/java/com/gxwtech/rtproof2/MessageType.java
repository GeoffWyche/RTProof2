package com.gxwtech.rtproof2;

import com.gxwtech.rtproof2.Messages.PumpAckMessageBody;
import com.gxwtech.rtproof2.Messages.UnknownMessageBody;

/**
 * Created by geoff on 5/29/16.
 */
public class MessageType {
    public static final short Alert           = 0x01;
    public static final short AlertCleared    = 0x02;
    public static final short DeviceTest      = 0x03;
    public static final short PumpStatus      = 0x04;
    public static final short PumpAck         = 0x06;
    public static final short PumpBackfill    = 0x08;
    public static final short FindDevice      = 0x09;
    public static final short DeviceLink      = 0x0a;
    public static final short ChangeTime      = 0x40;
    public static final short Bolus           = 0x42;
    public static final short ChangeTempBasal = 0x4c;
    public static final short ButtonPress     = 0x5b;
    public static final short PowerOn         = 0x5d;
    public static final short ReadTime        = 0x70;
    public static final short GetBattery      = 0x72;
    public static final short GetHistoryPage  = 0x80;
    public static final short GetPumpModel    = 0x8d;
    public static final short ReadTempBasal   = 0x98;
    public static final short ReadSettings    = 0xc0;

    public short mtype;
    public MessageType(short mtype) {
        this.mtype = mtype;
    }

    public static MessageBody constructMessageBody(MessageType messageType, byte[] bodyData) {
        switch (messageType.mtype) {
            case PumpAck: return new PumpAckMessageBody(bodyData);
            default: return new UnknownMessageBody(bodyData);
        }
    }
}
