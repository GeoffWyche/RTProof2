package com.gxwtech.rtproof2;

/**
 * Created by geoff on 5/29/16.
 */
public class PumpMessage {
    public PacketType packetType;
    public byte[] address;
    public MessageType messageType;
    public MessageBody messageBody;

    public PumpMessage() {

    }
    public void init(PacketType packetType, byte[] address, MessageType messageType, MessageBody messageBody) {
        this.packetType = packetType;
        this.address = address;
        this.messageType = messageType;
        this.messageBody = messageBody;
    }
    public void init(byte[] rxData) {
        if (rxData == null) {
            return;
        }
        if (rxData.length > 0) {
            this.packetType = new PacketType(rxData[0]);
        }
        if (rxData.length > 3) {
            this.address = ByteUtil.substring(rxData, 1, 3);
        }
        if (rxData.length > 4) {
            this.messageType = new MessageType(rxData[4]);
        }
        if (rxData.length > 5) {
            this.messageBody = MessageType.constructMessageBody(messageType, ByteUtil.substring(rxData, 5, rxData.length - 5));
        }
    }
    public byte[] getTxData() {
        byte[] rval = ByteUtil.concat(new byte[] {(byte)packetType.value},address);
        rval = ByteUtil.concat(rval,(byte)messageType.mtype);
        rval = ByteUtil.concat(rval,messageBody.getTxData());
        return rval;
    }

}
