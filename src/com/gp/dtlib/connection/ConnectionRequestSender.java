package com.gp.dtlib.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.DTConstants;
import com.gp.dtlib.DTDiscoveredClient;
import com.gp.dtlib.DTUtils;
import com.gp.dtlib.LibLog;

class ConnectionRequestSender {
    private static final String DEBUG_TAG = "Connector";

    private ConnectorCallBacks connectorCallBacks;
    private String myProfileName;
    private String myIpAddress;

    ConnectionRequestSender(ConnectorCallBacks connectorCallBacks){
        this.connectorCallBacks = connectorCallBacks;
    }

    void sendConnectionRequest(String myProfileName, DTDiscoveredClient discoveredClient) {
        this.myProfileName = myProfileName;
        myIpAddress = DTUtils.getLocalIpAddress();
        Thread connectionRequestSenderThread = new Thread(new ActualConnectionSender(discoveredClient.getIpAddress()));
        connectionRequestSenderThread.start();
    }

    class ActualConnectionSender implements Runnable{

        private String remoteIpAddress;

        private ActualConnectionSender(String remoteIpAddress) {
            this.remoteIpAddress = remoteIpAddress;
        }

        @Override
        public void run() {
            try {
                LibLog.d(DEBUG_TAG, "trying to connect with ip address ->" + remoteIpAddress);
                Socket socket = new Socket(remoteIpAddress, ConnectionManager.CONNECTING_PORT_NUMBER);
                
                LibLog.d(DEBUG_TAG, "sending connection stablished data with ->" + remoteIpAddress);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(connectionStublishedRequestInfo());
                dataOutputStream.flush();
                
                LibLog.d(DEBUG_TAG, "receiving connection stablished data with ->" + remoteIpAddress);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());
                String clientName = receivedMap.get(DTConstants.CLIENT_NAME);
                String clientIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                if(clientName == null || clientName.length() == 0 || clientIpAddress == null || clientIpAddress.length() == 0){
                	connectorCallBacks.connectorOnConnectionFailed("Connection Info Invalid");
                	socket.close();
                	return;
                }
                DTClient dtClients = new DTClient(clientName, clientIpAddress, socket);
                connectorCallBacks.connectorOnSuccessfulConnection(dtClients);
            } catch (Exception e) {
            	LibLog.d(DEBUG_TAG, "Connection failed for ipAddress = " + remoteIpAddress);
            	connectorCallBacks.connectorOnConnectionFailed("Connection Failed");
            }
        }
    }
    
    private String connectionStublishedRequestInfo(){
    	Map<String, String> sendInfo = new HashMap<>();
        sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
        sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
        sendInfo.put(DTConstants.CLIENT_WANT_TO_CONNECT, DTConstants.CLIENT_WANT_TO_CONNECT_YES);
        return DTUtils.mapToString(sendInfo);
    }

    interface ConnectorCallBacks{
        void connectorOnSuccessfulConnection(DTClient connectedClient);
        void connectorOnConnectionFailed(String message);
    }

}
