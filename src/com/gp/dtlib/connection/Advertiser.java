package com.gp.dtlib.connection;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.DTConstants;
import com.gp.dtlib.DTUtils;
import com.gp.dtlib.LibLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        myProfileName = "gobinda"; //TODO - later we will load this profile data from database
        myIpAddress = DTUtils.getLocalIpAddress();
        advertisingThread = new Thread(new ActualAdvertiser());
    }

    void startAdvertising(){
        if(isAdvertising){
            LibLog.d(DEBUG_TAG, "already advertising. [return]");
            return;
        }
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

            Map<String, String> sendInfo = new HashMap<>();
            sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
            sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);

            while(neededToAdvertising){
                try {
                    serverSocket = new ServerSocket(ConnectionManager.ADVERTISING_PORT_NUMBER);
                    serverSocket.setSoTimeout(10000);
                    advertisingCallBacks.advertiserStarted(ConnectionManager.ADVERTISING_PORT_NUMBER, DTUtils.getLocalIpAddress());
                    Socket socket = serverSocket.accept();

                    //first of all send own info
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
                    dataOutputStream.flush();
                    //dataOutputStream.close();

                    //then receive scanner's info
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    Map<String,String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());
                    if(Objects.equals(receivedMap.get(DTConstants.CLIENT_WANT_TO_CONNECT), DTConstants.CLIENT_WANT_TO_CONNECT_YES)){
                        String clientName = receivedMap.get(DTConstants.CLIENT_NAME);
                        String clientIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                        DTClient dtClients = new DTClient(clientName, clientIpAddress, socket);
                        LibLog.d(DEBUG_TAG, "got connected with clinet name = " + clientName);
                        advertisingCallBacks.advertiserGotConnected(dtClients);
                        break;
                    }
                } catch (Exception e) {
                	LibLog.d(DEBUG_TAG, "server socket hosting timed out");
                    e.printStackTrace();

                } finally {
                    try {serverSocket.close();} catch (Exception e1) { LibLog.d(DEBUG_TAG, "problem occurs while closing server socket");}
                }
            }
            isAdvertising = false;
            LibLog.d(DEBUG_TAG, "ActualAdvertiser -> advertising end.");
            advertisingCallBacks.advertiserStopped();
        }
    }

    public interface AdvertisingCallBacks {
        void advertiserStarted(int port, String ipAddress);
        void advertiserStopped();
        void advertiserGotConnected(DTClient connectedClients);
    }
}

