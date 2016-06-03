package com.gxwtech.rtproof2.RileyLinkBLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations.BLECommOperation;
import com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations.BLECommOperationResult;
import com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations.CharacteristicReadOperation;
import com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations.CharacteristicWriteOperation;
import com.gxwtech.rtproof2.RileyLinkBLE.BLECommOperations.DescriptorWriteOperation;
import com.gxwtech.rtproof2.Constants;
import com.gxwtech.rtproof2.util.HexDump;
import com.gxwtech.rtproof2.MainActivity;
import com.gxwtech.rtproof2.util.ThreadUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Created by geoff on 5/26/16.
 */
public class RileyLinkBLE {
    private static final String TAG = "RileyLinkBLE";
    public boolean gattDebugEnabled = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattCallback bluetoothGattCallback;

    private Context context;
    private BluetoothManager bluetoothManager;

    private BluetoothDevice rileyLinkDevice;
    private BluetoothGatt bluetoothConnectionGatt = null;

    private BLECommOperation mCurrentOperation;
    private Semaphore gattOperationSema = new Semaphore(1,true);

    private Runnable radioResponseCountNotified;

    public RileyLinkBLE() {
        this.context = MainActivity.getAppContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothGattCallback = new BluetoothGattCallback() {

            @Override
            public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if (gattDebugEnabled) {
                    Log.v(TAG, ThreadUtil.sig() + "onCharacteristicChanged " + GattAttributes.lookup(characteristic.getUuid()) + " " + HexDump.toHexString(characteristic.getValue()));
                    if (characteristic.getUuid().equals(UUID.fromString(GattAttributes.CHARA_RADIO_RESPONSE_COUNT))) {
                        Log.d(TAG, "Response Count is " + HexDump.toHexString(characteristic.getValue()));
                    }
                }
                if (radioResponseCountNotified != null) {
                    radioResponseCountNotified.run();
                }
            }

            @Override
            public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);


                final String statusMessage = getGattStatusMessage(status);
                if (gattDebugEnabled) {
                    Log.v(TAG, ThreadUtil.sig() + "onCharacteristicRead (" + GattAttributes.lookup(characteristic.getUuid()) + ") "
                            + statusMessage + ":" + HexDump.toHexString(characteristic.getValue()));
                }
                mCurrentOperation.gattOperationCompletionCallback(characteristic.getUuid(),characteristic.getValue());
            }

            @Override
            public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                final String uuidString = GattAttributes.lookup(characteristic.getUuid());
                if (gattDebugEnabled) {
                    Log.v(TAG, ThreadUtil.sig() + "onCharacteristicWrite " + getGattStatusMessage(status) + " " + uuidString + " " + HexDump.toHexString(characteristic.getValue()));
                }
                mCurrentOperation.gattOperationCompletionCallback(characteristic.getUuid(),characteristic.getValue());
            }

            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                // https://github.com/NordicSemiconductor/puck-central-android/blob/master/PuckCentral/app/src/main/java/no/nordicsemi/puckcentral/bluetooth/gatt/GattManager.java#L117
                if (status == 133) {
                    Log.e(TAG, "Got the status 133 bug, closing gatt");
                    disconnect();
                    return;
                }

                if (gattDebugEnabled) {
                    final String stateMessage;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        stateMessage = "CONNECTED";
                    } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                        stateMessage = "CONNECTING";
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        stateMessage = "DISCONNECTED";
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                        stateMessage = "DISCONNECTING";
                    } else {
                        stateMessage = "UNKNOWN (" + newState + ")";
                    }

                    Log.w(TAG, "onConnectionStateChange " + getGattStatusMessage(status) + " " + stateMessage);
                }

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.local.BLUETOOTH_CONNECTED));
                } else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.local.BLUETOOTH_DISCONNECTED));
                    disconnect();
                    Log.w(TAG, "Cannot establish Bluetooth connection.");
                }
            }


            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if (gattDebugEnabled) {
                    Log.w(TAG, "onDescriptorWrite "
                            + GattAttributes.lookup(descriptor.getUuid()) + " "
                            + getGattStatusMessage(status)
                            + " written: " + HexDump.toHexString(descriptor.getValue()));
                }
                mCurrentOperation.gattOperationCompletionCallback(descriptor.getUuid(),descriptor.getValue());
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                mCurrentOperation.gattOperationCompletionCallback(descriptor.getUuid(),descriptor.getValue());
                if (gattDebugEnabled) {
                    Log.w(TAG, "onDescriptorRead " + getGattStatusMessage(status) + " status " + descriptor);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                if (gattDebugEnabled) {
                    Log.w(TAG, "onMtuChanged " + mtu + " status " + status);
                }
            }

            @Override
            public void onReadRemoteRssi(final BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                if (gattDebugEnabled) {
                    Log.w(TAG, "onReadRemoteRssi " + getGattStatusMessage(status) + ": " + rssi);
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                if (gattDebugEnabled) {
                    Log.w(TAG, "onReliableWriteCompleted status " + status);
                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    final List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                        final UUID uuidService = service.getUuid();
                        final String uuidServiceString = uuidService.toString();
                        if (gattDebugEnabled) {
                            String debugString = "Found service: " + GattAttributes.lookup(uuidServiceString, "Unknown device") + " (" + uuidServiceString + ")";

                            for (BluetoothGattCharacteristic character : characteristics) {
                                final String uuidCharacteristicString = character.getUuid().toString();
                                debugString += "    - " + GattAttributes.lookup(uuidCharacteristicString);
                            }
                            Log.w(TAG, debugString);
                        }
                    }
                    if (gattDebugEnabled) {
                        Log.w(TAG, "onServicesDiscovered " + getGattStatusMessage(status));
                    }
                    Intent intent = new Intent(Constants.local.BLE_services_discovered);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else {
                    Log.e(TAG, "onServicesDiscovered " + getGattStatusMessage(status));
                }
            }
        };
    }

    public void registerRadioResponseCountNotification(Runnable notifier) {
        radioResponseCountNotified = notifier;
    }

    public void discoverServices() {
        if (bluetoothConnectionGatt.discoverServices()) {
            Log.w(TAG, "Starting to discover GATT Services.");

        } else {
            Log.e(TAG, "Cannot discover GATT Services.");
        }
    }

    public void findRileylink() {
        rileyLinkDevice =  bluetoothAdapter.getRemoteDevice(Constants.RileyLinkAddress);
        // if this succeeds, we get a connection state change callback?
        connectGatt();
    }

    // This function must be run on UI thread.
    public void connectGatt() {
        bluetoothConnectionGatt = rileyLinkDevice.connectGatt(context, false, bluetoothGattCallback);
        if (gattDebugEnabled) {
            Log.d(TAG, "Gatt Connected?");
        }
    }

    public void disconnect() {
        Log.w(TAG, "Closing GATT connection");
        // Close old conenction
        if (bluetoothConnectionGatt != null) {
            // Not sure if to disconnect or to close first..
            bluetoothConnectionGatt.disconnect();
            bluetoothConnectionGatt.close();
            bluetoothConnectionGatt = null;
        }
    }

    public BLECommOperationResult setNotification_blocking(UUID serviceUUID, UUID charaUUID) {
        BLECommOperationResult rval = new BLECommOperationResult();
        try {
            gattOperationSema.acquire();
            SystemClock.sleep(1); // attempting to yield thread, to make sequence of events easier to follow
        } catch (InterruptedException e) {
            Log.e(TAG,"setNotification_blocking: interrupted waiting for gattOperationSema");
            return rval;
        }
        if (mCurrentOperation != null) {
            rval.resultCode = BLECommOperationResult.RESULT_BUSY;
        } else {
            BluetoothGattCharacteristic chara = bluetoothConnectionGatt.getService(serviceUUID).getCharacteristic(charaUUID);
            // Tell Android that we want the notifications
            bluetoothConnectionGatt.setCharacteristicNotification(chara, true);
            List<BluetoothGattDescriptor> list = chara.getDescriptors();
            if (gattDebugEnabled) {
                for (int i = 0; i < list.size(); i++) {
                    Log.d(TAG, "Found descriptor: " + list.get(i).toString());
                }
            }
            BluetoothGattDescriptor descr = list.get(0);
            // Tell the remote device to send the notifications
            mCurrentOperation = new DescriptorWriteOperation(bluetoothConnectionGatt,descr,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mCurrentOperation.execute(this);
            if (mCurrentOperation.timedOut) {
                rval.resultCode = BLECommOperationResult.RESULT_TIMEOUT;
            } else if (mCurrentOperation.interrupted) {
                rval.resultCode = BLECommOperationResult.RESULT_INTERRUPTED;
            } else {
                rval.resultCode = BLECommOperationResult.RESULT_SUCCESS;
            }
        }
        mCurrentOperation = null;
        gattOperationSema.release();
        return rval;
    }

    // call from main
    public BLECommOperationResult writeCharacteristic_blocking(UUID serviceUUID, UUID charaUUID, byte[] value) {
        BLECommOperationResult rval = new BLECommOperationResult();
        rval.value = value;
        try {
            gattOperationSema.acquire();
            SystemClock.sleep(1); // attempting to yield thread, to make sequence of events easier to follow
        } catch (InterruptedException e) {
            Log.e(TAG,"writeCharacteristic_blocking: interrupted waiting for gattOperationSema");
            return rval;
        }
        if (mCurrentOperation != null) {
            rval.resultCode = BLECommOperationResult.RESULT_BUSY;
        } else {
            BluetoothGattCharacteristic chara = bluetoothConnectionGatt.getService(serviceUUID).getCharacteristic(charaUUID);
            mCurrentOperation = new CharacteristicWriteOperation(bluetoothConnectionGatt,chara,value);
            mCurrentOperation.execute(this);
            if (mCurrentOperation.timedOut) {
                rval.resultCode = BLECommOperationResult.RESULT_TIMEOUT;
            } else if (mCurrentOperation.interrupted) {
                rval.resultCode = BLECommOperationResult.RESULT_INTERRUPTED;
            } else {
                rval.resultCode = BLECommOperationResult.RESULT_SUCCESS;
            }
        }
        mCurrentOperation = null;
        gattOperationSema.release();
        return rval;
    }

    public BLECommOperationResult readCharacteristic_blocking(UUID serviceUUID, UUID charaUUID) {
        BLECommOperationResult rval = new BLECommOperationResult();
        try {
            gattOperationSema.acquire();
            SystemClock.sleep(1); // attempting to yield thread, to make sequence of events easier to follow
        } catch (InterruptedException e) {
            Log.e(TAG,"readCharacteristic_blocking: Interrupted waiting for gattOperationSema");
            return rval;
        }
        if (mCurrentOperation != null) {
            rval.resultCode = BLECommOperationResult.RESULT_BUSY;
        } else {
            BluetoothGattCharacteristic chara = bluetoothConnectionGatt.getService(serviceUUID).getCharacteristic(charaUUID);
            mCurrentOperation = new CharacteristicReadOperation(bluetoothConnectionGatt, chara);
            mCurrentOperation.execute(this);
            if (mCurrentOperation.timedOut) {
                rval.resultCode = BLECommOperationResult.RESULT_TIMEOUT;
            } else if (mCurrentOperation.interrupted) {
                rval.resultCode = BLECommOperationResult.RESULT_INTERRUPTED;
            } else {
                rval.resultCode = BLECommOperationResult.RESULT_SUCCESS;
                rval.value = mCurrentOperation.getValue();
            }
        }
        mCurrentOperation = null;
        gattOperationSema.release();
        return rval;
    }

    private String getGattStatusMessage(final int status) {
        final String statusMessage;
        if (status == BluetoothGatt.GATT_SUCCESS) {
            statusMessage = "SUCCESS";
        } else if (status == BluetoothGatt.GATT_FAILURE) {
            statusMessage = "FAILED";
        } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
            statusMessage = "NOT PERMITTED";
        } else if (status == 133) {
            statusMessage = "Found the strange 133 bug";
        } else {
            statusMessage = "UNKNOWN (" + status + ")";
        }

        return statusMessage;
    }


}
