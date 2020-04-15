package com.gobinda;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class DTAdvertiser {

    DTAdvertiser(DTAdvertiserCallBacks dtAdvertiserCallBacks, String myProfileName) {
        this.dtAdvertiserCallBacks = dtAdvertiserCallBacks;
        this.myProfileName = myProfileName;
        myIpAddress = DTUtils.getLocalIpAddress();
        advertisingThread = new Thread(new ActualAdvertiser());
    }

    void startAdvertising() {
        if(isAdvertising) {
            new Thread(() -> dtAdvertiserCallBacks.advertisingStartedAlready()).start();
            return;
        }
        if(myIpAddress == null || myIpAddress.length() < 7) {
            new Thread(() -> dtAdvertiserCallBacks.advertisingNotStated("Invalid Ip Address")).start();
            return;
        }
        if(myProfileName == null || myProfileName.length() < 3) {
            new Thread(() -> dtAdvertiserCallBacks.advertisingNotStated("Invalid Profile Name")).start();
            return;
        }
        neededToAdvertising = true;
        advertisingThread.start();
    }

    void stopAdvertising() {
        if(!isAdvertising){
            new Thread(() -> dtAdvertiserCallBacks.advertisingStoppedAlready()).start();
            return;
        }
        try {
            neededToAdvertising = false;
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface DTAdvertiserCallBacks {
        void advertisingStarted(int port, String ipAddress);
        void advertisingStartedAlready();
        void advertisingNotStated(String message);
        void advertisingStopped();
        void advertisingStoppedAlready();
        void connectedWithClient(DTConnectedClient connectedClient);
        void connectionFailed();
    }

    private DTAdvertiserCallBacks dtAdvertiserCallBacks;

    private boolean isAdvertising = false;
    private boolean neededToAdvertising = false;

    private Thread advertisingThread;
    private ServerSocket serverSocket;

    private String myProfileName;
    private String myIpAddress;

    class ActualAdvertiser implements Runnable {

        @Override
        public void run() {
            isAdvertising = true;

            Map<String, String> sendInfo = new HashMap<>();
            sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
            sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);

            while(neededToAdvertising){
                try {
                    serverSocket = new ServerSocket(DTConstants.ADVERTISING_PORT_NUMBER);
                    serverSocket.setSoTimeout(10000);
                    new Thread(() -> dtAdvertiserCallBacks.advertisingStarted(DTConstants.ADVERTISING_PORT_NUMBER, myIpAddress)).start();
                    Socket socket = serverSocket.accept();

                    //sending own info
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
                    dataOutputStream.flush();

                    //receive advertiser's info
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());
                    if(receivedMap.size() != 0){
                        String userWantToConnect = receivedMap.get(DTConstants.CLIENT_WANT_TO_CONNECT);
                        if(userWantToConnect != null && userWantToConnect.equals(DTConstants.CLIENT_WANT_TO_CONNECT_YES)){
                            String name = receivedMap.get(DTConstants.CLIENT_NAME);
                            String ipAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                            if(name != null && name.length() != 0 && ipAddress != null && ipAddress.length() != 0){
                                DTConnectedClient connectedClient = new DTConnectedClient(name, ipAddress, socket, DTConnectedClient.ConnectionRole.SERVER);
                                new Thread(() -> dtAdvertiserCallBacks.connectedWithClient(connectedClient)).start();
                                isAdvertising = false; // since we are returning from here
                                return;
                            }
                            new Thread(() -> dtAdvertiserCallBacks.connectionFailed()).start(); // since user want to be connected but failed
                        }
                    }

                } catch (Exception ignore) { } finally {
                    try {serverSocket.close();} catch (Exception ignore) { }
                }
            }
            isAdvertising = false;
            new Thread(() -> dtAdvertiserCallBacks.advertisingStopped()).start();
        }
    }
}
