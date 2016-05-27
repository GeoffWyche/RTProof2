package com.gxwtech.rtproof2;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.gxwtech.rtproof2.BLECommOperations.BLECommOperationResult;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by geoff on 5/26/16.
 */
public class RFSpyReader {
    private static final String TAG = "RFSpyReader";
    private Context context;
    private BLEComm bleComm;
    private Semaphore waitForRadioData = new Semaphore(0,true);
    AsyncTask<Void,Void,Void> readerTask;
    private LinkedBlockingQueue<byte[]> mDataQueue = new LinkedBlockingQueue<>();

    public RFSpyReader(Context context, BLEComm bleComm) {
        this.context = context;
        this.bleComm = bleComm;
    }

    // This timeout must be coordinated with the length of the RFSpy radio operation or Bad Things Happen.
    public byte[] poll(int timeout_ms) {
        try {
            // block until timeout or data available.
            // returns null if timeout.
            return mDataQueue.poll(timeout_ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG,"poll: Interrupted waiting for data");
        }
        return null;
    }

    // Call this from the "response count" notification handler.
    public void newDataIsAvailable() {
        waitForRadioData.release();
    }

    public void start() {
        readerTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                UUID serviceUUID = UUID.fromString(GattAttributes.SERVICE_RADIO);
                UUID radioDataUUID = UUID.fromString(GattAttributes.CHARA_RADIO_DATA);
                BLECommOperationResult result;
                while (true) {
                    try {
                        waitForRadioData.acquire();
                        SystemClock.sleep(1);
                        result = bleComm.readCharacteristic_blocking(serviceUUID, radioDataUUID);
                        if (result.resultCode == BLECommOperationResult.RESULT_SUCCESS) {
                            mDataQueue.add(result.value);
                        } else if (result.resultCode == BLECommOperationResult.RESULT_INTERRUPTED) {
                            Log.e(TAG, "Read operation was interrupted");
                        } else if (result.resultCode == BLECommOperationResult.RESULT_TIMEOUT) {
                            Log.e(TAG, "Read operation on Radio Data timed out");
                        } else if (result.resultCode == BLECommOperationResult.RESULT_BUSY) {
                            Log.e(TAG, "FAIL: BLEComm reports operation already in progress");
                        } else if (result.resultCode == BLECommOperationResult.RESULT_NONE) {
                            Log.e(TAG, "FAIL: got invalid result code: " + result.resultCode);
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for data");
                    }
                }
            }
        }.execute();
    }

}
