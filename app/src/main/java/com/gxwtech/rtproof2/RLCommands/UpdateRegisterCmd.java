package com.gxwtech.rtproof2.RLCommands;

/**
 * Created by geoff on 5/29/16.
 */
public class UpdateRegisterCmd extends CmdBase {
    public byte[] rawData;
    public byte[] rawResponse;

    public UpdateRegisterCmd(byte addr, byte val) {
        rawData = new byte[] {RILEYLINK_CMD_UPDATE_REGISTER, addr, val};
    }

    @Override
    public byte[] getRaw() {
        return rawData;
    }

    @Override
    public byte[] getRawResponse() {
        return rawResponse;
    }

}
