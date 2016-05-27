package com.gxwtech.rtproof2;

/**
 * Created by geoff on 5/22/16.
 */

public class RadioPacket {
    protected byte[] pkt;
    public RadioPacket(byte[] pkt) {
        this.pkt = pkt;
    }
    public byte[] getRaw() {
        return pkt;
    }
    public byte[] getEncoded() {
        byte[] withCRC = ByteUtil.concat(pkt,CRC.crc8(pkt));
        byte[] encoded = RileyLinkUtil.encodeData(withCRC);
        return encoded;
    }
}
