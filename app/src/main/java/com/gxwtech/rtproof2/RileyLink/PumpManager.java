package com.gxwtech.rtproof2.RileyLink;

import android.util.Log;

import com.gxwtech.rtproof2.medtronic.Messages.ButtonPressCarelinkMessageBody;
import com.gxwtech.rtproof2.util.ByteUtil;
import com.gxwtech.rtproof2.medtronic.Messages.MessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.MessageType;
import com.gxwtech.rtproof2.medtronic.PacketType;
import com.gxwtech.rtproof2.medtronic.PumpMessage;
import com.gxwtech.rtproof2.RileyLinkBLE.RadioPacket;
import com.gxwtech.rtproof2.RileyLinkBLE.RadioResponse;
import com.gxwtech.rtproof2.medtronic.Messages.GenericMessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.GetHistoryPageCarelinkMessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.GetPumpModelCarelinkMessageBody;
import com.gxwtech.rtproof2.medtronic.PumpData.HistoryReport;


/**
 * Created by geoff on 5/30/16.
 */
public class PumpManager {
    private static final String TAG = "PumpManager";
    public double[] scanFrequencies = {916.45, 916.50, 916.55, 916.60, 916.65, 916.70, 916.75, 916.80};
    private final RFSpy rfspy;
    private byte[] pumpID;
    public boolean DEBUG_PUMPMANAGER = true;
    public PumpManager(RFSpy rfspy, byte[] pumpID) {
        this.rfspy = rfspy;
        this.pumpID = pumpID;
    }

    protected PumpMessage sendAndListen(PumpMessage msg) {
        return sendAndListen(msg,2000);
    }

    protected PumpMessage sendAndListen(PumpMessage msg, int timeout_ms) {
        RFSpyResponse resp = rfspy.transmitThenReceive(new RadioPacket(msg.getTxData()),timeout_ms);
        PumpMessage rval = new PumpMessage(resp.getRadioResponse().getPayload());
        return rval;
    }

    public HistoryReport getPumpHistory(int pageNumber) {
        wakeup(6);
        PumpMessage getHistoryMsg = makePumpMessage(new MessageType(MessageType.CMD_M_READ_HISTORY), new GetHistoryPageCarelinkMessageBody(pageNumber));
        RFSpyResponse response = rfspy.transmitThenReceive(new RadioPacket(getHistoryMsg.getTxData()),(byte)0,(byte)0,(byte)0,(byte)0,15000,(byte)0);
        Log.e(TAG,"getPumpHistory: raw response is " + ByteUtil.shortHexString(response.getRaw()));
        // makePumpMessage() // +++ NYI
        return new HistoryReport();
    }

    // See ButtonPressCarelinkMessageBody
    public void pressButton(int which) {
        wakeup(6);
        PumpMessage pressButtonMessage = makePumpMessage(new MessageType(MessageType.CMD_M_KEYPAD_PUSH),new ButtonPressCarelinkMessageBody(which));
        PumpMessage resp = sendAndListen(pressButtonMessage);
        if (resp.messageType.mtype != MessageType.PumpAck) {
            Log.e(TAG,"Pump did not ack button press.");
        }
    }

    private void wakeup(int duration_minutes) {
        PumpMessage msg = makePumpMessage(new MessageType(MessageType.PowerOn),new GenericMessageBody(new byte[] {(byte)duration_minutes}));
        RFSpyResponse resp = rfspy.transmitThenReceive(new RadioPacket(msg.getTxData()),(byte)0,(byte)200,(byte)0,(byte)0,15000,(byte)0);
        Log.e(TAG,"wakeup: raw response is "+ByteUtil.shortHexString(resp.getRaw()));
    }

    public void tunePump() {
        scanForPump(scanFrequencies);
    }

    private void scanForPump(double[] frequencies) {
        wakeup(1);
        FrequencyScanResults results = new FrequencyScanResults();

        for (int i=0; i<frequencies.length; i++) {
            int tries = 3;
            FrequencyTrial trial = new FrequencyTrial();
            trial.frequencyMHz = frequencies[i];
            int sumRSSI = 0;
            for (int j = 0; j<tries; j++) {
                PumpMessage msg = makePumpMessage(new MessageType(MessageType.GetPumpModel), new GetPumpModelCarelinkMessageBody());
                rfspy.setBaseFrequency(frequencies[i]);
                RFSpyResponse resp = rfspy.transmitThenReceive(new RadioPacket(msg.getTxData()),(byte) 0, (byte) 0, (byte) 0, (byte) 0, rfspy.EXPECTED_MAX_BLUETOOTH_LATENCY_MS, (byte) 0);
                if (resp.wasTimeout()) {
                    Log.e(TAG, String.format("scanForPump: Failed to find pump at frequency %.2f", frequencies[i]));
                } else if (resp.looksLikeRadioPacket()) {
                    RadioResponse radioResponse = new RadioResponse(resp.getRaw());
                    if (radioResponse.isValid()) {
                        sumRSSI += radioResponse.rssi;
                        trial.successes++;
                    } else {
                        Log.w(TAG,"Failed to parse radio response: " + ByteUtil.shortHexString(resp.getRaw()));
                    }
                } else {
                    Log.e(TAG, "scanForPump: raw response is " + ByteUtil.shortHexString(resp.getRaw()));
                }
                trial.tries++;
            }
            sumRSSI += -99.0 * (trial.tries - trial.successes);
            trial.averageRSSI = (double)(sumRSSI) / (double)(trial.tries);
            results.trials.add(trial);
        }
        results.sort(); // sorts in ascending order
        Log.d(TAG,"Sorted scan results:");
        for (int k=0; k<results.trials.size(); k++) {
            FrequencyTrial one = results.trials.get(k);
            Log.d(TAG,String.format("Scan Result[%d]: Freq=%.2f, avg RSSI = %f",k,one.frequencyMHz, one.averageRSSI));
        }
        FrequencyTrial bestTrial = results.trials.get(results.trials.size()-1);
        results.bestFrequencyMHz = bestTrial.frequencyMHz;
        if (bestTrial.successes > 0) {
            rfspy.setBaseFrequency(results.bestFrequencyMHz);
        } else {
            Log.e(TAG,"No pump response during scan.");
        }
    }

    private PumpMessage makePumpMessage(MessageType messageType, MessageBody messageBody) {
        PumpMessage msg = new PumpMessage();
        msg.init(new PacketType(PacketType.Carelink),pumpID,messageType,messageBody);
        return msg;
    }
}
