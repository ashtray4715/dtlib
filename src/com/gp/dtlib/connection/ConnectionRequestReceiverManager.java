package com.gp.dtlib.connection;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.LibLog;
import com.gp.dtlib.connection.ConnectionRequestReceiver.ConnectionRequestReceiverCallBacks;

class ConnectionRequestReceiverManager {
    private static String DEBUG_TAG = "ConnectionRequestReceiverManager";

    private static ConnectionRequestReceiverManager connectionRequestReceiverManager;

    static void initialize(ConnectionRequestReceiverManagerCallBacks connectionRequestReceiverManagerCallBacks){
        if(connectionRequestReceiverManager != null){
            LibLog.d(DEBUG_TAG, "initialize -> ConnectionRequestReceiverManager already initialized. [return]");
            return;
        }
        connectionRequestReceiverManager = new ConnectionRequestReceiverManager(connectionRequestReceiverManagerCallBacks);
    }

    static ConnectionRequestReceiverManager getInstance(){
        if(connectionRequestReceiverManager == null){
        	LibLog.d(DEBUG_TAG, "getInstance -> ConnectionRequestReceiverManager not initialized. [return]");
            return null;
        }
        return connectionRequestReceiverManager;
    }

    private ConnectionRequestReceiver connectionRequestReceiver;
    private ConnectionRequestReceiverManagerCallBacks connectionRequestReceiverManagerCallBacks;

    private ConnectionRequestReceiverManager(ConnectionRequestReceiverManagerCallBacks connectionRequestReceiverManagerCallBacks){
        this.connectionRequestReceiverManagerCallBacks = connectionRequestReceiverManagerCallBacks;
    }

    void startConnectionRequestReceiving(String myProfileName){
        connectionRequestReceiver = new ConnectionRequestReceiver(new ConnectionRequestReceiverCallBackHandler());
        connectionRequestReceiver.startConnectionRequestReceiving(myProfileName);
    }

    void stopConnectionRequestReceiving(){
        if(connectionRequestReceiver != null) {
        	connectionRequestReceiver.stopConnectionRequestReceiving();
        }
    }

    class ConnectionRequestReceiverCallBackHandler implements ConnectionRequestReceiverCallBacks {

		@Override
		public void startedReceiving(int port, String ipAddress) {
			connectionRequestReceiverManagerCallBacks.connectionRequestReceiverStartedReceiving(port, ipAddress);
		}

		@Override
		public void stoppedReceiving() {
			connectionRequestReceiver = null;
	        connectionRequestReceiverManagerCallBacks.connectionRequestReceiverStoppedReceiving();
		}

		@Override
		public void gotConnectedWithClient(DTClient connectedClient) {
			connectionRequestReceiverManagerCallBacks.connectionRequestReceiverGotConnectedWithClient(connectedClient);
		}
        
    }

    public interface ConnectionRequestReceiverManagerCallBacks{
    	void connectionRequestReceiverStartedReceiving(int port, String ipAddress);
        void connectionRequestReceiverStoppedReceiving();
        void connectionRequestReceiverGotConnectedWithClient(DTClient connectedClient);
    }
}
