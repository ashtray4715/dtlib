package com.gobinda;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class DTScanner {

    DTScanner(DTScannerCallBacks scannerCallBacks, String myProfileName){
        this.scannerCallBacks = scannerCallBacks;
        this.myProfileName = myProfileName;
    }

    public interface DTScannerCallBacks{
        void scanningStarted();
        void scanningStartedAlready();
        void scanningNotStarted(String message);
        void scanningStopped();
        void scanningStoppedAlready();
        void discoveredClient(DTDiscoveredClient discoveredClient);
    }

    void startScanning() {
        if(isScanning){
            new Thread(() -> scannerCallBacks.scanningStartedAlready()).start();
            return;
        }
        isScanning = true;

        myIpAddress = DTUtils.getLocalIpAddress();
        if(myIpAddress == null){
            new Thread(() -> scannerCallBacks.scanningNotStarted("Invalid Ip Address")).start();
            return;
        }

        synchronized (lockForScanningEndForTotalIpAddress){
            lastScanningStartTime = DTUtils.getCurrentTimeAsInteger();
            scanningEndForTotalIpAddress = 0;
        }
        new Thread(() -> scannerCallBacks.scanningStarted()).start();

        String myIpWithoutLastIntButDot = myIpAddress.substring(0, myIpAddress.lastIndexOf(".") + 1);
        for(int i=0;i<=255;i++){
            Thread th = new Thread(new ActualScanner(myIpWithoutLastIntButDot + i, lastScanningStartTime));
            th.start();
        }
    }

    void stopScanning() {
        if(!isScanning){
            new Thread(() -> scannerCallBacks.scanningStoppedAlready()).start();
            return;
        }
        //we can't stop scanning since we can't control that thread
        //we just simply ignore responses that will comes from previous request by changing the scanning start time
        synchronized (lockForScanningEndForTotalIpAddress){
            lastScanningStartTime = DTUtils.getCurrentTimeAsInteger();
            isScanning = false;
        }
        new Thread(() -> scannerCallBacks.scanningStopped()).start();
    }

    private DTScannerCallBacks scannerCallBacks;
    private String myProfileName;
    private String myIpAddress;

    private int scanningEndForTotalIpAddress = 0;
    private final Object lockForScanningEndForTotalIpAddress = new Object();
    private long lastScanningStartTime = 0;

    private boolean isScanning;


    class ActualScanner implements Runnable{
        private String ipAddress;
        private long scanningStartTime;

        private ActualScanner(String ipAddress, long scanningStartTime){
            this.ipAddress = ipAddress;
            this.scanningStartTime = scanningStartTime;
        }

        @Override
        public void run() {

            Map<String, String> sendInfo = new HashMap<>();
            sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
            sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
            sendInfo.put(DTConstants.CLIENT_WANT_TO_CONNECT, DTConstants.CLIENT_WANT_TO_CONNECT_NO);

            DTDiscoveredClient willDiscoveredClient = null;

            try {
                Socket socket = new Socket(ipAddress, DTConstants.ADVERTISING_PORT_NUMBER);

                //receive advertiser's info
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());

                //sending own info
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
                dataOutputStream.flush();

                //processing advertiser's info
                String recvName = receivedMap.get(DTConstants.CLIENT_NAME);
                String recvIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                if(recvName != null && recvName.length() >= 3 && recvIpAddress != null && recvIpAddress.length() >= 7) {
                    willDiscoveredClient = new DTDiscoveredClient(recvName, recvIpAddress);
                }

                socket.close();
            } catch (Exception ignore) {}

            synchronized (lockForScanningEndForTotalIpAddress){

                //check if the response is for a previous request [then we have to ignore it]
                if(scanningStartTime != lastScanningStartTime){
                    return;
                }

                if(willDiscoveredClient != null && scannerCallBacks != null){
                    DTDiscoveredClient finalWillDiscoveredClient = willDiscoveredClient;
                    new Thread(() -> scannerCallBacks.discoveredClient(finalWillDiscoveredClient)).start();
                }

                scanningEndForTotalIpAddress++;
                if(scanningEndForTotalIpAddress == 256){
                    isScanning = false;
                    if(scannerCallBacks != null){
                        new Thread(() -> scannerCallBacks.scanningStopped()).start();
                    }
                }
            }
        }
    }

}
