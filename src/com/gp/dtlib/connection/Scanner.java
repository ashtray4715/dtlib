package com.gp.dtlib.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.gp.dtlib.DTConstants;
import com.gp.dtlib.DTDiscoveredClient;
import com.gp.dtlib.DTErrors;
import com.gp.dtlib.DTUtils;
import com.gp.dtlib.LibLog;

class Scanner{
    private static final String DEBUG_TAG = "Scanner";

    private String myProfileName;
    private String myIpAddress;

    private static int scanningEndForTotalIpAddress = 0;
    private final Object lockForScanningEndForTotalIpAddress = new Object();
    private static long scanningStartTime = 0;

    private static boolean isScanning;

    private ScanningCallBacks scanningCallBacks;

    Scanner(ScanningCallBacks scanningCallBacks){
        this.scanningCallBacks = scanningCallBacks;
    }

    void startScanning(){
        myProfileName = "gobinda"; //TODO - later we will load this data from db
        myIpAddress = DTUtils.getLocalIpAddress();

        if(isScanning){
        	LibLog.d(DEBUG_TAG, "already scanning so [return]");
            return;
        }
        isScanning = true;

        String myIp = DTUtils.getLocalIpAddress();
        if(myIp == null){
            scanningCallBacks.onErrorOccurs(DTErrors.ScanningError.IP_NOT_FOUND);
            return;
        }

        scanningEndForTotalIpAddress = 0;
        synchronized (lockForScanningEndForTotalIpAddress){
            scanningStartTime = DTUtils.getCurrentTimeAsInteger();
        }
        scanningCallBacks.onStartedScanning();
        for(int i=0;i<=255;i++){
            String currentIp = myIp.substring(0, myIp.lastIndexOf(".") + 1) + i;
            Thread th = new Thread(new ConnectWithIp(currentIp, scanningStartTime));
            th.start();
        }
    }

    void stopScanning(){
        if(!isScanning){
        	LibLog.d(DEBUG_TAG, "already stopped scanning so [return]");
            return;
        }
        //we can't stop scanning since we can't control that thread
        //we just simply ignore responses that will comes from previous request by changing the scanning start time
        synchronized (lockForScanningEndForTotalIpAddress){
            scanningStartTime = DTUtils.getCurrentTimeAsInteger();
            isScanning = false;
            scanningCallBacks.onStoppedScanning();
        }
    }

    class ConnectWithIp implements Runnable{
        private String ipAddress;
        private long scanningStartTime;

        private ConnectWithIp(String ipAddress, long scanningStartTime){
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
                Socket socket = new Socket(ipAddress, ConnectionManager.ADVERTISING_PORT_NUMBER);
                //LogHandler.d(DEBUG_TAG, "found open socket. ip address ->" + ipAddress);

                //first of all receive advertiser's info
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());

                //then send own info
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
                dataOutputStream.flush();
                //dataOutputStream.close();

                String clientName = receivedMap.get(DTConstants.CLIENT_NAME);
                String clientIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                willDiscoveredClient = new DTDiscoveredClient(clientName, clientIpAddress);

                socket.close();
            } catch (Exception e) {
                //LogHandler.d(DEBUG_TAG, "not found ip address ->" + ipAddress);
            }
            synchronized (lockForScanningEndForTotalIpAddress){
                //check if the response is for a previous request [then we have to ignore it]
                if(this.scanningStartTime != Scanner.scanningStartTime){
                	LibLog.d(DEBUG_TAG, "Ignoring previous scanning response for ipAddress " + ipAddress);
                    return;
                }
                if(willDiscoveredClient != null && willDiscoveredClient.getName() != null){
                	LibLog.d(DEBUG_TAG, "discoved client ip address is " + willDiscoveredClient.getIpAddress());
                    scanningCallBacks.onDiscover(willDiscoveredClient);
                }
                scanningEndForTotalIpAddress++;
                if(scanningEndForTotalIpAddress == 256){
                    isScanning = false;
                    scanningCallBacks.onStoppedScanning();
                }
            }
        }
    }

    public interface ScanningCallBacks{
        void onStartedScanning();
        void onStoppedScanning();
        void onDiscover(DTDiscoveredClient discoveredClient);
        void onErrorOccurs(DTErrors.ScanningError scanningError);
    }
}
