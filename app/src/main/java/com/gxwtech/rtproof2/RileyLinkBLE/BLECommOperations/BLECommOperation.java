package com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations;

import com.gxwtech.rtproof2.RileyLinkBLE.RileyLinkBLE;

import java.util.UUID;

/**
 * Created by geoff on 5/26/16.
 */
public abstract class BLECommOperation {
    public boolean timedOut = false;
    public boolean interrupted = false;

    // This is to be run on the main thread
    public abstract void execute(RileyLinkBLE comm);
    public void gattOperationCompletionCallback(UUID uuid, byte[] value) {}
    public int getGattOperationTimeout_ms() { return 22000;}

    public byte[] getValue() { return null; }
}
