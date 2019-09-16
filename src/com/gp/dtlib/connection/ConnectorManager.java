package com.gp.dtlib.connection;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.LibLog;

class ConnectorManager {
    private static final String DEBUG_TAG = "ConnectorManager";

    private static ConnectorManager connectorManager;
    static void initialize(ConnectorManagerCallBacks connectorManagerCallBacks){
        if(connectorManager != null){
        	LibLog.d(DEBUG_TAG, "initialize -> ConnectorManager already initialized. [return]");
            return;
        }
        connectorManager = new ConnectorManager(connectorManagerCallBacks);
    }
    static ConnectorManager getInstance() {
        if(connectorManager == null){
        	LibLog.d(DEBUG_TAG, "getInstance -> ConnectorManager not initialized. [return]");
            return null;
        }
        return connectorManager;
    }

    private Connector connector;
    private ConnectorManagerCallBacks connectorManagerCallBacks;

    private ConnectorManager(ConnectorManagerCallBacks connectorManagerCallBacks){
        this.connectorManagerCallBacks = connectorManagerCallBacks;
    }

    void sendConnectionRequest(String ipAddress) {
        connector = new Connector(new ConnectorCallBackHandler());
        connector.sendConnectionRequest(ipAddress);
    }

    private class ConnectorCallBackHandler implements Connector.ConnectorCallBacks{

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
