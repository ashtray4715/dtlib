package com.gp.dtlib.connection;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.LibLog;

class AdvertiserManager {
    private static String DEBUG_TAG = "AdvertiserManager";

    private static AdvertiserManager advertiseManager;

    static void initialize(AdvertiserManagerCallBacks advertiseManagerCallBacks){
        if(advertiseManager != null){
            LibLog.d(DEBUG_TAG, "initialize -> AdvertiserManager already initialized. [return]");
            return;
        }
        advertiseManager = new AdvertiserManager(advertiseManagerCallBacks);
    }

    static AdvertiserManager getInstance(){
        if(advertiseManager == null){
        	LibLog.d(DEBUG_TAG, "getInstance -> AdvertiserManager not initialized. [return]");
            return null;
        }
        return advertiseManager;
    }

    private Advertiser advertiser;
    private AdvertiserManagerCallBacks advertiseManagerCallBacks;

    private AdvertiserManager(AdvertiserManagerCallBacks advertiseManagerCallBacks){
        this.advertiseManagerCallBacks = advertiseManagerCallBacks;
    }

    void startAdvertising(){
        advertiser = new Advertiser(new AdvertisingCallBackHandler());
        advertiser.startAdvertising();
    }

    void stopAdvertising(){
        if(advertiser != null) {
            advertiser.stopAdvertising();
        }
    }

    class AdvertisingCallBackHandler implements Advertiser.AdvertisingCallBacks {

        @Override
        public void advertiserStarted(int port, String ipAddress) {
            advertiseManagerCallBacks.advertiserManagerStartedAdvertising();
        }

        @Override
        public void advertiserStopped() {
            advertiser = null;
            advertiseManagerCallBacks.advertiserManagerStoppedAdvertising();
        }

        @Override
        public void advertiserGotConnected(DTClient connectedClients) {
            advertiseManagerCallBacks.advertiserManagerGotConnected(connectedClients);
        }
    }

    public interface AdvertiserManagerCallBacks{
        void advertiserManagerStartedAdvertising();
        void advertiserManagerStoppedAdvertising();
        void advertiserManagerGotConnected(DTClient connectedClient);
    }
}
