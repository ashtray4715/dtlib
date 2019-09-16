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

class ConnectionRequestReceiver{
    private static final String DEBUG_TAG = "ConnectionRequestReceiver";

    private static boolean isReceiving = false;

    private ConnectionRequestReceiverCallBacks connectionRequestReceiverCallBacks;

    private boolean neededReceiving = false;

    private Thread connectionRequestReceivingThread;
    private ServerSocket serverSocket;
    
    private String myProfileName;
    private String myIpAddress;

    ConnectionRequestReceiver(ConnectionRequestReceiverCallBacks connectionRequestReceiverCallBacks){
        this.connectionRequestReceiverCallBacks = connectionRequestReceiverCallBacks;
        connectionRequestReceivingThread = new Thread(new ActualConnectionRequestReceiver());
    }

    void startConnectionRequestReceiving(String myProfileName){
        if(isReceiving){
            LibLog.d(DEBUG_TAG, "already receiving connection request. [return]");
            return;
        }
        this.myProfileName = myProfileName;
        this.myIpAddress = DTUtils.getLocalIpAddress();
        neededReceiving = true;
        connectionRequestReceivingThread.start();
    }

    void stopConnectionRequestReceiving(){
        if(!isReceiving){
            LibLog.d(DEBUG_TAG, "connection request receiver stopped already. [return]");
            return;
        }
        try {
            neededReceiving = false;
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ActualConnectionRequestReceiver implements Runnable{

        @Override
        public void run() {
            isReceiving = true;
            while(neededReceiving){
                try {
                    serverSocket = new ServerSocket(ConnectionManager.ADVERTISING_PORT_NUMBER);
                    serverSocket.setSoTimeout(10000);
                    connectionRequestReceiverCallBacks.startedReceiving(ConnectionManager.CONNECTING_PORT_NUMBER, DTUtils.getLocalIpAddress());
                    Socket socket = serverSocket.accept();
                    
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    Map<String, String> receivedMap = DTUtils.stringToMap(dataInputStream.readUTF());
                    String clientName = receivedMap.get(DTConstants.CLIENT_NAME);
                    String clientIpAddress = receivedMap.get(DTConstants.CLIENT_IP_ADDRESS);
                    String userWantToConnect = receivedMap.get(DTConstants.CLIENT_WANT_TO_CONNECT);
                    
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(connectionReqeustAcceptInfo());
                    dataOutputStream.flush();
                    
                    if(clientName != null && clientName.length() != 0 && clientIpAddress != null && clientIpAddress.length() != 0 && userWantToConnect != null && userWantToConnect.equals(DTConstants.CLIENT_WANT_TO_CONNECT_YES)){
                    	 DTClient dtClients = new DTClient(clientName, clientIpAddress, socket);
                         connectionRequestReceiverCallBacks.gotConnectedWithClient(dtClients);
                         break;
                    }
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
            isReceiving = false;
            LibLog.d(DEBUG_TAG, "ActualConnectionRequestReceiver -> receiving end.");
            connectionRequestReceiverCallBacks.stoppedReceiving();
        }
        
    }
    
    private String connectionReqeustAcceptInfo(){
    	Map<String, String> sendInfo = new HashMap<>();
        sendInfo.put(DTConstants.CLIENT_NAME, myProfileName);
        sendInfo.put(DTConstants.CLIENT_IP_ADDRESS, myIpAddress);
        return DTUtils.mapToString(sendInfo);
    }
    
    public interface ConnectionRequestReceiverCallBacks {
        void startedReceiving(int port, String ipAddress);
        void stoppedReceiving();
        void gotConnectedWithClient(DTClient connectedClient);
    }
}

