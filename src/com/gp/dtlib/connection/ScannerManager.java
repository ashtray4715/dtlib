package com.gp.dtlib.connection;

import com.gp.dtlib.DTDiscoveredClient;
import com.gp.dtlib.DTErrors;
import com.gp.dtlib.LibLog;

public class ScannerManager {
    private static String DEBUG_TAG = "ScannerManager";

    private static ScannerManager scannerManager;

    public static void initialize(ScannerManagerCallBacks scannerManagerCallBacks){
        if(scannerManager != null){
        	LibLog.d(DEBUG_TAG, "initialize -> ScannerManager already initialized. [return]");
            return;
        }
        scannerManager = new ScannerManager(scannerManagerCallBacks);
    }

    public static ScannerManager getInstance(){
        if(scannerManager == null){
        	LibLog.d(DEBUG_TAG, "getInstance -> ScannerManager not initialized. [return]");
            return null;
        }
        return scannerManager;
    }

    private Scanner scanner;
    private ScannerManagerCallBacks scannerManagerCallBacks;

    private ScannerManager(ScannerManagerCallBacks scannerManagerCallBacks){
        this.scannerManagerCallBacks = scannerManagerCallBacks;
    }

    public void startScanning(){
        scanner = new Scanner(new ScanningCallBackHandler());
        scanner.startScanning();
    }

    public void stopScanning(){
        if(scanner != null) {
            scanner.stopScanning();
        }
    }

    private class ScanningCallBackHandler implements Scanner.ScanningCallBacks {

        @Override
        public void onStartedScanning() {
            scannerManagerCallBacks.scannerManagerStartedScanning();
        }

        @Override
        public void onStoppedScanning() {
            scannerManagerCallBacks.scannerManagerStoppedScanning();
            scanner = null;
        }

        @Override
        public void onDiscover(DTDiscoveredClient discoveredClient) {
            scannerManagerCallBacks.scannerManagerDiscoveredClients(discoveredClient);
        }

        @Override
        public void onErrorOccurs(DTErrors.ScanningError scanningError) {
            scannerManagerCallBacks.scannerManagerFoundError(scanningError);
            scanner = null;
        }
    }

    public interface ScannerManagerCallBacks{
        void scannerManagerStartedScanning();
        void scannerManagerStoppedScanning();
        void scannerManagerDiscoveredClients(DTDiscoveredClient discoveredClient);
        void scannerManagerFoundError(DTErrors.ScanningError scanningError);
    }

}
