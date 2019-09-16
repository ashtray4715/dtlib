package com.gp.dtlib.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.DTConstants;
import com.gp.dtlib.DTUtils;
import com.gp.dtlib.LibLog;

class Connector {
    private static final String DEBUG_TAG = "Connector";

    private ConnectorCallBacks connectorCallBacks;
    private String myProfileName;
    private String myIpAddress;

    Connector(ConnectorCallBacks connectorCallBacks){
        this.connectorCallBacks = connectorCallBacks;
    }

    void sendConnectionRequest(String ipAddress) {
        myProfileName = "gobinda"; // TODO - later we will load this info from db
        myIpAddress = DTUtils.getLocalIpAddress();
        Thread connectionRequestSenderThread = new Thread(new ActualConnectionSender(ipAddress));
        connectionRequestSenderThread.start();
    }

    class ActualConnectionSender implements Runnable{

        private String remoteIpAddress;

        private ActualConnectionSender(String removeIpAddress) {
            this.remoteIpAddress = removeIpAddress;
        }

        @Override
        public void run() {
            Map<String, String> sendInfo = new HashMap<>();
            sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
            sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
            sendInfo.put(DTConstants.CLIENT_WANT_TO_CONNECT, DTConstants.CLIENT_WANT_TO_CONNECT_YES);

            try {
                //LogHandler.d(DEBUG_TAG, "trying to connect with ip address ->" + ipAddress);
                Socket socket = new Socket(remoteIpAddress, ConnectionManager.ADVERTISING_PORT_NUMBER);
                LibLog.d(DEBUG_TAG, "sending connection request ip address ->" + remoteIpAddress);

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
                DTClient dtClients = new DTClient(clientName, clientIpAddress, socket);
                connectorCallBacks.connectorOnSuccessfulConnection(dtClients);
                return;
            } catch (Exception e) {
            	LibLog.d(DEBUG_TAG, "Connection failed for ipAddress = " + remoteIpAddress);
            }
            connectorCallBacks.connectorOnConnectionFailed("Connection Failed");
        }
    }

    interface ConnectorCallBacks{
        void connectorOnSuccessfulConnection(DTClient connectedClient);
        void connectorOnConnectionFailed(String message);
    }

}
