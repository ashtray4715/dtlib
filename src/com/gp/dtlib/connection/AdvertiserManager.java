package com.gp.dtlib.connection;

import com.gp.dtlib.LibLog;

public class AdvertiserManager {
    private static String DEBUG_TAG = "AdvertiserManager";

    private static AdvertiserManager advertiseManager;

    public static void initialize(AdvertiserManagerCallBacks advertiseManagerCallBacks){
        if(advertiseManager != null){
            LibLog.d(DEBUG_TAG, "initialize -> AdvertiserManager already initialized. [return]");
            return;
        }
        advertiseManager = new AdvertiserManager(advertiseManagerCallBacks);
    }

    public static AdvertiserManager getInstance(){
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

    public void startAdvertising(String myProfileName){
        advertiser = new Advertiser(new AdvertisingCallBackHandler());
        advertiser.startAdvertising(myProfileName);
    }

    public void stopAdvertising(){
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
        
    }

    public interface AdvertiserManagerCallBacks{
        void advertiserManagerStartedAdvertising();
        void advertiserManagerStoppedAdvertising();
    }
}
