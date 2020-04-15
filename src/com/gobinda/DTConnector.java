package com.gobinda;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class DTConnector {

    interface DTConnectorCallBacks {
        void connectionSuccessful(DTConnectedClient connectedClient);
        void connectionFailed(String message);
    }

    DTConnector(DTConnectorCallBacks dtConnectorCallBacks, String myProfileName){
        this.dtConnectorCallBacks = dtConnectorCallBacks;
        this.myProfileName = myProfileName;
    }

    void sendConnectionRequest(DTDiscoveredClient discoveredClient) {
        myIpAddress = DTUtils.getLocalIpAddress();
        if(myIpAddress == null || myIpAddress.length() < 7) {
            new Thread(() -> dtConnectorCallBacks.connectionFailed("Invalid Ip Address")).start();
            return;
        }
        if(myProfileName == null || myProfileName.length() < 3) {
            new Thread(() -> dtConnectorCallBacks.connectionFailed("Invalid Profile Name")).start();
            return;
        }
        new Thread(new ActualConnector(discoveredClient.getIpAddress())).start();
    }

    private DTConnectorCallBacks dtConnectorCallBacks;
    private String myProfileName;
    private String myIpAddress;

    class ActualConnector implements Runnable {

        private String remoteIpAddress;

        private ActualConnector(String removeIpAddress) {
            this.remoteIpAddress = removeIpAddress;
        }

        @Override
        public void run() {
            Map<String, String> sendInfo = new HashMap<>();
            sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
            sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
            sendInfo.put(DTConstants.CLIENT_WANT_TO_CONNECT, DTConstants.CLIENT_WANT_TO_CONNECT_YES);

            try {
                Socket socket = new Socket(remoteIpAddress, DTConstants.CONNECTING_PORT_NUMBER);

                //first of all receive advertiser's info
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());

                //then send own info
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
                dataOutputStream.flush();

                //process received info
                String clientName = receivedMap.get(DTConstants.CLIENT_NAME);
                String clientIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                DTConnectedClient dtClients = new DTConnectedClient(clientName, clientIpAddress, socket, DTConnectedClient.ConnectionRole.CLIENT);
                new Thread(() -> dtConnectorCallBacks.connectionSuccessful(dtClients)).start();
                return;
            } catch (Exception ignore) {}
            new Thread(() -> dtConnectorCallBacks.connectionFailed("Client not available"));
        }
    }


}