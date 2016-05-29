package com.gxwtech.rtproof2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.gxwtech.rtproof2.BLECommOperations.BLECommOperation;
import com.gxwtech.rtproof2.BLECommOperations.BLECommOperationResult;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="MainActivity";
    private static Context context;

    private BluetoothAdapter bluetoothAdapter;

    private Handler scanHandler = new Handler();
    private BroadcastReceiver mBroadcastReceiver;

    private BLEComm bleComm;
    private RFSpy rfspy;

    private BluetoothDevice deviceFoundFromScan;

    public MainActivity() {
    }

    public static Context getAppContext() {
        return MainActivity.context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.context = getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    Log.e(TAG,"onReceive: received null intent");
                } else {
                    String action = intent.getAction();
                    if (action == null) {
                        Log.e(TAG,"onReceive: null action");
                    } else {
                        Log.d(TAG,"onReceive: action=" + action);
                        if (Constants.rileylink_ready.equals(intent.getAction())) {
                            rfspy = new RFSpy(context,bleComm);
                            rfspy.startReader();
                            testRileyLink();
                            /*
                        } else if (Constants.start_ble_scan.equals(intent.getAction())) {
                            scanLeDevice(true);
                            */
                        } else if (Constants.ble_permission_granted.equals(intent.getAction())) {
                            bleComm.findRileylink();
                        } else if (Constants.rileylink_found.equals(intent.getAction())) {
                            bleComm.connectGatt();
                        } else if (Constants.local.BLUETOOTH_CONNECTED.equals(intent.getAction())) {
                            scanLeDevice(false);
                            bleComm.discoverServices();
                        } else if (Constants.local.BLE_services_discovered.equals((intent.getAction()))) {
                            BLECommOperationResult result = bleComm.setNotification_blocking(
                                    UUID.fromString(GattAttributes.SERVICE_RADIO),
                                    UUID.fromString(GattAttributes.CHARA_RADIO_RESPONSE_COUNT));
                            if (result.resultCode != BLECommOperationResult.RESULT_SUCCESS) {
                                Log.e(TAG,"Error setting response count notification");
                            }
                            Log.i(TAG,"Announcing RileyLink open For business");
                            Intent rlReady = new Intent(Constants.rileylink_ready);
                            LocalBroadcastManager.getInstance(MainActivity.context).sendBroadcast(rlReady);
                        } else {
                            Log.e(TAG,"Unhandled intent: " + intent.getAction());
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        // Keep these in the order in which they are expected to occur, for sanity.
        intentFilter.addAction(Constants.ble_permission_granted);
        //intentFilter.addAction(Constants.start_ble_scan);
        intentFilter.addAction(Constants.rileylink_found);
        intentFilter.addAction(Constants.local.BLUETOOTH_CONNECTED);
        intentFilter.addAction(Constants.local.BLE_services_discovered);
        intentFilter.addAction(Constants.rileylink_ready);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBroadcastReceiver,intentFilter);

        bleComm = new BLEComm();

        //Intent startScanIntent = new Intent(Constants.start_ble_scan);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startScanIntent);
        Log.d(TAG,"onCreate(): I'm alive");
        initBluetooth();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.activityResult.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = bluetoothManager.getAdapter();
                Intent intent = new Intent(Constants.ble_permission_granted);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
    }

    private void initBluetooth() {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.activityResult.REQUEST_ENABLE_BT);
        } else {
            if (bluetoothAdapter == null) {
                Log.e(TAG,"initBluetooth: adapter is null");
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = bluetoothManager.getAdapter();
            }
            Intent intent = new Intent(Constants.ble_permission_granted);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

    }

    public void testRileyLink() {

        // This operation reads from the BLE113 version.
        BLECommOperationResult result =
                bleComm.readCharacteristic_blocking(UUID.fromString(GattAttributes.SERVICE_RADIO),UUID.fromString(GattAttributes.CHARA_RADIO_VERSION));
        if (result.resultCode == BLECommOperationResult.RESULT_SUCCESS) {
            Log.d(TAG,"testRileyLink: version returned: " + ByteUtil.shortHexString(result.value));
        } else {
            Log.e(TAG,"testRileyLink: error, result code is "+result.resultCode);
        }

        rfspy.tunePump();

        /*
        // This operation reads from the CC1110 version
        // It also tests that the write-radio/get notification/read-radio sequence is working.
        RFSpyResponse response = rfspy.getRadioVersion();
        Log.d(TAG,"getRadioVersion response was: " + ByteUtil.shortHexString(response.getRaw()));

        //listenContinuously();
        shoutForPump(0,1);
        pressDownKey(0);
        SystemClock.sleep(1000);

        shoutForPump(1,1);
        pressDownKey(1);
        SystemClock.sleep(1000);

        shoutForPump(2,1);
        pressDownKey(2);
*/
    }

    public void onPressKeyButton(View view) {
        pressDownKey(0);
    }

    public void shoutForPump(int channel, int repeatCount) {
        Log.w(TAG,String.format("ShoutForPump, channel %d, repeatCount %d",channel,repeatCount));
        for (int i=0; i< repeatCount; i++) {
            rfspy.transmit(new RadioPacket(new byte[]{(byte) 0xa7, 0x51, (byte) 0x81, 0x63, 0x5d, 0x00}), (byte) channel, (byte) repeatCount, (byte) 0);
            SystemClock.sleep(100);
        }
    }

    public void pressDownKey(int channel) {
        Log.w(TAG,String.format("pressDownKey, channel %d",channel));
        rfspy.transmit(new RadioPacket(new byte[] {(byte)0xa7, 0x51, (byte)0x81, 0x63, 0x5b, 0x01, 0x04}),(byte)channel,(byte)0,(byte)0);
        SystemClock.sleep(100);
    }

    public void listenContinuously() {
        while (true) {
            RFSpyResponse response = rfspy.receive((byte) 0, 5000, (byte) 0);
            if (response.getRaw() == null) {
                Log.e(TAG,"listenContinuously: got null response");
            } else {
                Log.w(TAG,"listenContinuously: got response: " + ByteUtil.shortHexString(response.getRaw()));
                if (response.getRaw()[0] == (byte)0xbb) {
                    Log.e(TAG,"RileyLink interrupted, bailing.");
                    break;
                }
            }
        }
    }


    // Stops scanning after 100 seconds.
    private static final long SCAN_PERIOD = 100000;
    private boolean mScanning;
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.d(TAG, String.format("onLeScan: found device %s, rssi=%d, scanRecord=%s",
                            device.toString(), rssi, ByteUtil.shortHexString(scanRecord)));
                    // Can only run device.connectGatt from UI thread.
                    // So store the device and send an intent
                    deviceFoundFromScan = device;
                    Intent intent = new Intent(Constants.rileylink_found);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
            };


}
