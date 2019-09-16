package com.gp.dtlib.connection;

import com.gp.dtlib.DTConstants;
import com.gp.dtlib.DTUtils;
import com.gp.dtlib.LibLog;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class Advertiser{
    private static final String DEBUG_TAG = "Advertiser";

    private static boolean isAdvertising = false;

    private AdvertisingCallBacks advertisingCallBacks;

    private boolean neededToAdvertising = false;

    private Thread advertisingThread;
    private ServerSocket serverSocket;
    
    private String myProfileName;
    private String myIpAddress;

    Advertiser(AdvertisingCallBacks advertisingCallBacks){
        this.advertisingCallBacks = advertisingCallBacks;
        advertisingThread = new Thread(new ActualAdvertiser());
    }

    void startAdvertising(String myProfileName){
        if(isAdvertising){
            LibLog.d(DEBUG_TAG, "already advertising. [return]");
            return;
        }
        this.myProfileName = myProfileName;
        this.myIpAddress = DTUtils.getLocalIpAddress();
        neededToAdvertising = true;
        advertisingThread.start();
    }

    void stopAdvertising(){
        if(!isAdvertising){
            LibLog.d(DEBUG_TAG, "advertising stopped already. [return]");
            return;
        }
        try {
            neededToAdvertising = false;
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ActualAdvertiser implements Runnable{

        @Override
        public void run() {
            isAdvertising = true;
            while(neededToAdvertising){
                try {
                    serverSocket = new ServerSocket(ConnectionManager.ADVERTISING_PORT_NUMBER);
                    serverSocket.setSoTimeout(10000);
                    advertisingCallBacks.advertiserStarted(ConnectionManager.ADVERTISING_PORT_NUMBER, DTUtils.getLocalIpAddress());
                    Socket socket = serverSocket.accept();

                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(prepareAdvertisingInfo());
                    dataOutputStream.flush();
                    dataOutputStream.close();
                } catch (Exception e) {
                	LibLog.d(DEBUG_TAG, "server socket hosting timed out");
                } finally {
                    try {
                    	serverSocket.close();
                    } catch (Exception e1) {
                    	LibLog.d(DEBUG_TAG, "problem occurs while closing server socket");
                    }
                }
            }
            isAdvertising = false;
            LibLog.d(DEBUG_TAG, "ActualAdvertiser -> advertising end.");
            advertisingCallBacks.advertiserStopped();
        }
        
    }
    
    private String prepareAdvertisingInfo(){
    	Map<String, String> sendInfo = new HashMap<>();
        sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
        sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
        return DTUtils.mapToString(sendInfo);
    }
    
    public interface AdvertisingCallBacks {
        void advertiserStarted(int port, String ipAddress);
        void advertiserStopped();
    }
}

