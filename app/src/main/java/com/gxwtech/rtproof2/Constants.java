package com.gxwtech.rtproof2;

/**
 * Created by geoff on 5/21/16.
 */
public class Constants {
    public static final String AppPrefix = "com.gxwtech.rtproof";
    public class local {
        public static final String Prefix = ".local";
        public static final String BLUETOOTH_CONNECTED = AppPrefix + Prefix + ".BLUETOOTH_CONNECTED";
        public static final String BLUETOOTH_DISCONNECTED = AppPrefix + Prefix + ".BLUETOOTH_DISCONNECTED";
        public static final String BLE_services_discovered =  AppPrefix + Prefix + ".BLE_services_discovered";
    }
    public static final String rileylink_ready = AppPrefix + ".rileylink_ready";
    public static final String start_ble_scan = AppPrefix + ".start_ble_scan";
    public static final String ble_permission_granted = AppPrefix + ".ble_permission_granted";
    public static final String rileylink_found = AppPrefix + ".rileylink_found";
    public class activityResult {
        public static final int REQUEST_ENABLE_BT = 1;
    }
    public static final String RileyLinkAddress = "00:07:80:2D:9E:F4";
    //public static final String RileyLinkAddress = "00:07:80:39:4C:B1";
}
