package com.gxwtech.rtproof2;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.gxwtech.rtproof2.BLECommOperations.BLECommOperation;
import com.gxwtech.rtproof2.BLECommOperations.BLECommOperationResult;

import java.util.UUID;
import java.util.concurrent.RunnableFuture;

/**
 * Created by geoff on 5/26/16.
 */
public class RFSpy {
    public static final byte RFSPY_GET_STATE = 1;
    public static final byte RFSPY_GET_VERSION = 2;
    public static final byte RFSPY_GET_PACKET = 3; // aka Listen, receive
    public static final byte RFSPY_SEND = 4;
    public static final byte RFSPY_SEND_AND_LISTEN = 5;
    public static final byte RFSPY_UPDATE_REGISTER = 6;
    public static final byte RFSPY_RESET = 7;

    public static final int bluetoothLatency_ms = 22000;

    private static final String TAG = "RFSpy";
    private BLEComm bleComm;
    private RFSpyReader reader;
    private Context context;
    UUID radioServiceUUID = UUID.fromString(GattAttributes.SERVICE_RADIO);
    UUID radioDataUUID = UUID.fromString(GattAttributes.CHARA_RADIO_DATA);
    UUID radioVersionUUID = UUID.fromString(GattAttributes.CHARA_RADIO_VERSION);
    UUID responseCountUUID = UUID.fromString(GattAttributes.CHARA_RADIO_RESPONSE_COUNT);

    public RFSpy(Context context, BLEComm bleComm) {
        this.context = context;
        this.bleComm = bleComm;
        reader = new RFSpyReader(context,bleComm);
    }

    // Call this after the RL services are discovered.
    // Starts an async task to read when data is available
    public void startReader() {
        bleComm.registerRadioResponseCountNotification(new Runnable() {
            @Override
            public void run() {
                newDataIsAvailable();
            }
        });
        reader.start();
    }

    // Call this from the "response count" notification handler.
    public void newDataIsAvailable() {
        // pass the message to the reader (which should be internal to RFSpy)
        reader.newDataIsAvailable();
    }

    // This gets the version from the BLE113, not from the CC1110.
    // I.e., this gets the version from the BLE interface, not from the radio.
    public String getVersion() {
        BLECommOperationResult result = bleComm.readCharacteristic_blocking(radioServiceUUID,radioVersionUUID);
        if (result.resultCode == BLECommOperationResult.RESULT_SUCCESS) {
            return StringUtil.fromBytes(result.value);
        } else {
            Log.e(TAG,"getVersion failed with code: "+result.resultCode);
            return "(null)";
        }
    }

    // The caller has to know how long the RFSpy will be busy with what was sent to it.
    private RFSpyResponse writeToData(byte[] bytes, int responseTimeout_ms) {
        SystemClock.sleep(100);
        // FIXME drain read queue?
        byte[] junkInBuffer = reader.poll(0);

        while (junkInBuffer != null) {
            Log.w(TAG,ThreadUtil.sig()+"writeToData: draining read queue, found this: " + ByteUtil.shortHexString(junkInBuffer));
            junkInBuffer = reader.poll(0);
        }

        // prepend length, and send it.
        byte[] prepended = ByteUtil.concat(new byte[] {(byte)(bytes.length)},bytes);
        bleComm.writeCharacteristic_blocking(radioServiceUUID,radioDataUUID,prepended);
        SystemClock.sleep(100);
        Log.w(TAG,ThreadUtil.sig()+"writeToData: 'timeout' is " + (responseTimeout_ms + bluetoothLatency_ms));
        byte[] rawResponse = reader.poll(responseTimeout_ms + bluetoothLatency_ms);
        if (rawResponse == null) {
            Log.e(TAG,"No response from RileyLink");
        }
        return new RFSpyResponse(rawResponse);
    }

    public RFSpyResponse getRadioVersion() {
        RFSpyResponse resp = writeToData(new byte[] {RFSPY_GET_VERSION},1000);
        if (resp == null) {
            Log.e(TAG,"getRadioVersion returned null");
        }
        /*
        Log.d(TAG,"checking response count");
        BLECommOperationResult checkRC = bleComm.readCharacteristic_blocking(radioServiceUUID,responseCountUUID);
        if (checkRC.resultCode == BLECommOperationResult.RESULT_SUCCESS) {
            Log.d(TAG,"Response count is: " + ByteUtil.shortHexString(checkRC.value));
        } else {
            Log.e(TAG,"Error getting response count, code is " + checkRC.resultCode);
        }
        */
        return resp;
    }

    public RFSpyResponse transmit(RadioPacket radioPacket, byte sendChannel, byte repeatCount, byte delay_ms) {
        // append checksum, encode data, send it.
        byte[] fullPacket = ByteUtil.concat(new byte[] {RFSPY_SEND,sendChannel,repeatCount, delay_ms},radioPacket.getEncoded());
        RFSpyResponse response = writeToData(radioPacket.getEncoded(),repeatCount * delay_ms);
        return response;
    }

    public RFSpyResponse receive(byte listenChannel, int timeout_ms, byte retryCount) {
        int receiveDelay = timeout_ms * (retryCount+1);
        byte[] listen = {RFSPY_GET_PACKET,listenChannel,
                (byte)((timeout_ms >> 24)&0x0FF),
                (byte)((timeout_ms >> 16)&0x0FF),
                (byte)((timeout_ms >> 8)&0x0FF),
                (byte)(timeout_ms & 0x0FF),
                retryCount};
        return writeToData(listen,receiveDelay);
    }

    public RFSpyResponse transmitThenReceive(byte sendChannel, byte repeatCount, byte delay_ms, byte listenChannel, int timeout_ms, byte retryCount) {
        int sendDelay = repeatCount * delay_ms * 1; // let 1ms be base time to send a packet at all.
        int receiveDelay = timeout_ms * (retryCount + 1);
        byte[] sendAndListen = {RFSPY_SEND_AND_LISTEN,sendChannel,repeatCount,delay_ms,listenChannel,
                (byte)((timeout_ms >> 24)&0x0FF),
                (byte)((timeout_ms >> 16)&0x0FF),
                (byte)((timeout_ms >> 8)&0x0FF),
                (byte)(timeout_ms & 0x0FF),
                retryCount};
        return writeToData(sendAndListen, sendDelay + receiveDelay);
    }

    public void updateRegister() {

    }
    public void setBaseFrequency() {

    }

}
