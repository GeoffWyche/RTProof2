package com.gxwtech.rtproof2.RileyLink;

import android.os.SystemClock;
import android.util.Log;

import com.gxwtech.rtproof2.medtronic.Messages.ButtonPressCarelinkMessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.PumpAckMessageBody;
import com.gxwtech.rtproof2.medtronic.PumpData.Page;
import com.gxwtech.rtproof2.medtronic.PumpData.records.Record;
import com.gxwtech.rtproof2.medtronic.PumpModel;
import com.gxwtech.rtproof2.util.ByteUtil;
import com.gxwtech.rtproof2.medtronic.Messages.MessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.MessageType;
import com.gxwtech.rtproof2.medtronic.PacketType;
import com.gxwtech.rtproof2.medtronic.PumpMessage;
import com.gxwtech.rtproof2.RileyLinkBLE.RadioPacket;
import com.gxwtech.rtproof2.RileyLinkBLE.RadioResponse;
import com.gxwtech.rtproof2.medtronic.Messages.CarelinkShortMessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.GetHistoryPageCarelinkMessageBody;
import com.gxwtech.rtproof2.medtronic.Messages.GetPumpModelCarelinkMessageBody;
import com.gxwtech.rtproof2.medtronic.PumpData.HistoryReport;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by geoff on 5/30/16.
 */
public class PumpManager {
    private static final String TAG = "PumpManager";
    public double[] scanFrequencies = {916.45, 916.50, 916.55, 916.60, 916.65, 916.70, 916.75, 916.80};
    private long pumpAwakeUntil = 0;
    private final RFSpy rfspy;
    private byte[] pumpID;
    public boolean DEBUG_PUMPMANAGER = true;
    public PumpManager(RFSpy rfspy, byte[] pumpID) {
        this.rfspy = rfspy;
        this.pumpID = pumpID;
    }

    private PumpMessage runCommandWithArgs(PumpMessage msg) {
        PumpMessage rval;
        PumpMessage shortMessage = makePumpMessage(msg.messageType,new CarelinkShortMessageBody(new byte[]{0}));
        // look for ack from short message
        PumpMessage shortResponse = sendAndListen(shortMessage);
        if (shortResponse.messageType.mtype == MessageType.PumpAck) {
            rval = sendAndListen(msg);
            return rval;
        } else {
            Log.e(TAG,"runCommandWithArgs: Pump did not ack Attention packet");
        }
        return new PumpMessage();
    }

    protected PumpMessage sendAndListen(PumpMessage msg) {
        return sendAndListen(msg,2000);
    }

    protected PumpMessage sendAndListen(PumpMessage msg, int timeout_ms) {
        boolean showPumpMessages = true;
        if (showPumpMessages) {
            Log.i(TAG,"Sent:"+ByteUtil.shortHexString(msg.getTxData()));
        }
        RFSpyResponse resp = rfspy.transmitThenReceive(new RadioPacket(msg.getTxData()),timeout_ms);
        PumpMessage rval = new PumpMessage(resp.getRadioResponse().getPayload());
        if (showPumpMessages) {
            Log.i(TAG,"Received:"+ByteUtil.shortHexString(resp.getRadioResponse().getPayload()));
        }
        return rval;
    }

    public Page getPumpHistoryPage(int pageNumber) {
        RawHistoryPage rval = new RawHistoryPage();
        wakeup(6);
        PumpMessage getHistoryMsg = makePumpMessage(new MessageType(MessageType.CMD_M_READ_HISTORY), new GetHistoryPageCarelinkMessageBody(pageNumber));
        //
        //PumpMessage msg = makePumpMessage(new byte[]{MessageType.CMD_M_READ_HISTORY,1,(byte)pageNumber,2,2});
        //PumpMessage msg = makePumpMessage(new MessageType(MessageType.CMD_M_READ_HISTORY),tryit);
        Log.i(TAG,"getPumpHistoryPage("+pageNumber+"): "+ByteUtil.shortHexString(getHistoryMsg.getTxData()));
        PumpMessage firstResponse = runCommandWithArgs(getHistoryMsg);
        Log.i(TAG,"getPumpHistoryPage("+pageNumber+"): " + ByteUtil.shortHexString(firstResponse.getContents()));
        PumpMessage ackMsg = makePumpMessage(MessageType.PumpAck,new PumpAckMessageBody());
        GetHistoryPageCarelinkMessageBody currentResponse = new GetHistoryPageCarelinkMessageBody(firstResponse.getMessageBody().getTxData());
        int expectedFrameNum = 1;
        while (expectedFrameNum == currentResponse.getFrameNumber()) {
            expectedFrameNum++;
            Log.i(TAG,"getPumpHistoryPage: Got frame "+currentResponse.getFrameNumber());
            rval.appendData(currentResponse.getFrameData());
            PumpMessage nextMsg = sendAndListen(ackMsg);
            //Log.i(TAG, "getPumpHistoryPage: pump's reply to our sent ack: " + ByteUtil.shortHexString(nextMsg.getContents()));
            currentResponse = new GetHistoryPageCarelinkMessageBody(nextMsg.getMessageBody().getTxData());
        }
        if (rval.getLength() != 1024) {
            Log.w(TAG,"getPumpHistoryPage: short page.  Expected length of 1024, found length of "+rval.getLength());
        }
        if (rval.isChecksumOK() == false) {
            Log.e(TAG,"getPumpHistoryPage: checksum is wrong");
        }

        rval.dumpToDebug();

        Page page = new Page();
        page.parseFrom(rval.getData(),PumpModel.MM522);

        return page;
    }

    public ArrayList<Page> getAllHistoryPages() {
        ArrayList<Page> pages = new ArrayList<>();
        for (int pageNum = 0; pageNum < 16; pageNum++) {
            pages.add(getPumpHistoryPage(pageNum));
        }
        return pages;
    }

    public ArrayList<Page> getHistoryEventsSinceDate(Instant when) {
        ArrayList<Page> pages = new ArrayList<>();
        for (int pageNum = 0; pageNum < 16; pageNum++) {
            pages.add(getPumpHistoryPage(pageNum));
            for (Page page : pages) {
                for (Record r : page.mRecordList) {
                    LocalDateTime timestamp = r.getTimestamp().getLocalDateTime();
                    Log.i(TAG, "Found record: (" + r.getClass().getSimpleName() + ") " + timestamp.toString());
                }
            }
        }
        return pages;
    }

    public void testPageDecode() {
        byte[] raw = new byte[] {(byte)0x6D, (byte)0x62, (byte)0x10, (byte)0x05, (byte)0x0C, (byte)0x00, (byte)0xE8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x00, (byte)0xE8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x63, (byte)0x10, (byte)0x6D, (byte)0x63, (byte)0x10, (byte)0x05, (byte)0x0C, (byte)0x00, (byte)0xE8, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x00, (byte)0xE8, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x5A, (byte)0xA5, (byte)0x49, (byte)0x04, (byte)0x10, (byte)0x01, (byte)0x01, (byte)0x01,
                (byte)0x00, (byte)0x6D, (byte)0xA5, (byte)0x49, (byte)0x04, (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xB5, (byte)0x6E};
        Page page = new Page();
        page.parseFrom(raw,PumpModel.MM522);
        Log.i(TAG,"testPageDecode: done");
    }

    public void getPumpRTC() {
        wakeup(6);
        PumpMessage getRTCMsg = makePumpMessage(new MessageType(MessageType.CMD_M_READ_RTC), new CarelinkShortMessageBody(new byte[]{0}));
        Log.i(TAG,"getPumpRTC: " + ByteUtil.shortHexString(getRTCMsg.getTxData()));
        RFSpyResponse response = rfspy.transmitThenReceive(new RadioPacket(getRTCMsg.getTxData()),2000);
        Log.i(TAG,"getPumpRTC response: " + ByteUtil.shortHexString(response.getRadioResponse().getPayload()));
    }

    public void getPumpModel() {
        wakeup(6);
        PumpMessage msg = makePumpMessage(new MessageType(MessageType.GetPumpModel), new GetPumpModelCarelinkMessageBody());
        Log.i(TAG,"getPumpModel: " + ByteUtil.shortHexString(msg.getTxData()));
        PumpMessage response = sendAndListen(msg);
        Log.i(TAG,"getPumpModel response: " + ByteUtil.shortHexString(response.getContents()));
    }

    public void tryoutPacket(byte[] pkt) {
        sendAndListen(makePumpMessage(pkt));
    }

    public void hunt() {
        tryoutPacket(new byte[] {MessageType.CMD_M_READ_PUMP_STATUS,0});
        tryoutPacket(new byte[] {MessageType.CMD_M_READ_FIRMWARE_VER,0});
        tryoutPacket(new byte[] {MessageType.CMD_M_READ_INSULIN_REMAINING,0});

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

    public void wakeup(int duration_minutes) {
        if (SystemClock.elapsedRealtime() > pumpAwakeUntil) {
            Log.i(TAG,"Waking pump...");
            PumpMessage msg = makePumpMessage(new MessageType(MessageType.PowerOn), new CarelinkShortMessageBody(new byte[]{(byte) duration_minutes}));
            RFSpyResponse resp = rfspy.transmitThenReceive(new RadioPacket(msg.getTxData()), (byte) 0, (byte) 200, (byte) 0, (byte) 0, 15000, (byte) 0);
            Log.i(TAG, "wakeup: raw response is " + ByteUtil.shortHexString(resp.getRaw()));
            pumpAwakeUntil = SystemClock.elapsedRealtime() + duration_minutes * 60 * 1000;
        }
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
/*
        // Use ternary search to find frequency with maximum RSSI.
func searchFrequencies(pump *medtronic.Pump) uint32 {
        pump.SetRetries(1)
        lower := startFreq
        upper := endFreq
        for {
                delta := upper - lower
                if delta < precision {
                        return (lower + upper) / 2
                }
                delta /= 3
                lowerThird := lower + delta
                r1 := tryFrequency(pump, lowerThird)
                upperThird := upper - delta
                r2 := tryFrequency(pump, upperThird)
                if r1 < r2 {
                        lower = lowerThird
                } else {
                        upper = upperThird
                }
        }
}

_
*/

    }

    private PumpMessage makePumpMessage(MessageType messageType, MessageBody messageBody) {
        PumpMessage msg = new PumpMessage();
        msg.init(new PacketType(PacketType.Carelink),pumpID,messageType,messageBody);
        return msg;
    }

    private PumpMessage makePumpMessage(byte msgType, MessageBody body) {
        return makePumpMessage(new MessageType(msgType),body);
    }

    private PumpMessage makePumpMessage(byte[] typeAndBody) {
        PumpMessage msg = new PumpMessage();
        msg.init(ByteUtil.concat(ByteUtil.concat(new byte[]{(byte)0xa7},pumpID),typeAndBody));
        return msg;
    }
}
