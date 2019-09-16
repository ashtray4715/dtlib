package com.gp.dtlib.connection;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.DTDiscoveredClient;
import com.gp.dtlib.LibLog;

class ConnectionRequestSenderManager {
    private static final String DEBUG_TAG = "ConnectorManager";

    private static ConnectionRequestSenderManager connectorManager;
    static void initialize(ConnectorManagerCallBacks connectorManagerCallBacks){
        if(connectorManager != null){
        	LibLog.d(DEBUG_TAG, "initialize -> ConnectorManager already initialized. [return]");
            return;
        }
        connectorManager = new ConnectionRequestSenderManager(connectorManagerCallBacks);
    }
    static ConnectionRequestSenderManager getInstance() {
        if(connectorManager == null){
        	LibLog.d(DEBUG_TAG, "getInstance -> ConnectorManager not initialized. [return]");
            return null;
        }
        return connectorManager;
    }

    private ConnectionRequestSender connector;
    private ConnectorManagerCallBacks connectorManagerCallBacks;

    private ConnectionRequestSenderManager(ConnectorManagerCallBacks connectorManagerCallBacks){
        this.connectorManagerCallBacks = connectorManagerCallBacks;
    }

    void sendConnectionRequest(String myProfileName, DTDiscoveredClient discoveredClient) {
        connector = new ConnectionRequestSender(new ConnectorCallBackHandler());
        connector.sendConnectionRequest(myProfileName, discoveredClient);
    }

    private class ConnectorCallBackHandler implements ConnectionRequestSender.ConnectorCallBacks{

        @Override
        public void connectorOnSuccessfulConnection(DTClient connectedClient) {
            connector = null;
            connectorManagerCallBacks.connectionManagerOnSuccessfulConnected(connectedClient);
        }

        @Override
        public void connectorOnConnectionFailed(String message) {
            connector = null;
            connectorManagerCallBacks.connectionManagerOnConnectionFailed(message);
        }
    }

    interface ConnectorManagerCallBacks{
        void connectionManagerOnSuccessfulConnected(DTClient connectedClient);
        void connectionManagerOnConnectionFailed(String message);
    }
}
