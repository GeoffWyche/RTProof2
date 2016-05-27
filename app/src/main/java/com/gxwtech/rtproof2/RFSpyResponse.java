package com.gxwtech.rtproof2;

/**
 * Created by geoff on 5/26/16.
 */
public class RFSpyResponse {
    protected byte[] raw;

    public RFSpyResponse() {
    }

    public RFSpyResponse(byte[] bytes) {
        raw = bytes;
    }

    public byte[] getRaw() {
        return raw;
    }
}
