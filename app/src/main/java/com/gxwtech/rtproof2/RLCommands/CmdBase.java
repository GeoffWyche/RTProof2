package com.gxwtech.rtproof2.RLCommands;

/**
 * Created by geoff on 5/29/16.
 */
public class CmdBase {
    public static final byte RILEYLINK_CMD_GET_STATE       = 1;
    public static final byte RILEYLINK_CMD_GET_VERSION     = 2;
    public static final byte RILEYLINK_CMD_GET_PACKET      = 3;
    public static final byte RILEYLINK_CMD_SEND_PACKET     = 4;
    public static final byte RILEYLINK_CMD_SEND_AND_LISTEN = 5;
    public static final byte RILEYLINK_CMD_UPDATE_REGISTER = 6;
    public static final byte RILEYLINK_CMD_RESET           = 7;

    public byte[] getRaw() { return new byte[] {}; }
    public byte[] getRawResponse() { return new byte[] {}; }
}
