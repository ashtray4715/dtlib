package com.gp.dtlib.connection;

import java.util.Objects;

import com.gp.dtlib.DTClient;
import com.gp.dtlib.DTDiscoveredClient;
import com.gp.dtlib.DTErrors;
import com.gp.dtlib.LibLog;
import com.gp.dtlib.connection.ConnectionRequestReceiverManager.ConnectionRequestReceiverManagerCallBacks;

public class ConnectionManager {
    private static final String DEBUG_TAG = "ConnectionManager";
    static final int ADVERTISING_PORT_NUMBER = 55555;
    static final int CONNECTING_PORT_NUMBER = 55556;

    private static ConnectionManager connectionManager;
    public static ConnectionManager getInstance(){
        if(connectionManager == null)
            connectionManager = new ConnectionManager();
        return connectionManager;
    }

    private ConnectionManagerAdvertisingCallBacks connectionManagerAdvertisingCallBacks;
    private ConnectionManagerScanningCallBacks connectionManagerScanningCallBacks;
    private ConnectionManagerConnectionRequestSenderCallBacks connectionManagerConnectionRequestSenderCallBacks;
    private ConnectionManagerConnectionRequestReceiverCallBacks connectionManagerConnectionRequestReceiverCallBacks;

    private ConnectionManager(){
        AdvertiserManager.initialize(new AdvertiseManagerCallBackHandler());
        ScannerManager.initialize(new ScannerManagerCallBackHandler());
        ConnectionRequestSenderManager.initialize(new ConnectionRequestSenderManagerCallBackHandler());
        ConnectionRequestReceiverManager.initialize(new ConnectionRequestReceiverManagerCallBackHandler());
    }

    public void startAdvertising(String myProfileName, ConnectionManagerAdvertisingCallBacks connectionManagerAdvertisingCallBacks, ConnectionManagerConnectionRequestReceiverCallBacks connectionManagerConnectionRequestReceiverCallBacks){
    	LibLog.d(DEBUG_TAG, "startAdvertising");
        this.connectionManagerAdvertisingCallBacks = connectionManagerAdvertisingCallBacks;
        this.connectionManagerConnectionRequestReceiverCallBacks = connectionManagerConnectionRequestReceiverCallBacks;
        Objects.requireNonNull(AdvertiserManager.getInstance()).startAdvertising(myProfileName);
        Objects.requireNonNull(ConnectionRequestReceiverManager.getInstance()).startConnectionRequestReceiving(myProfileName);
    }

    public void stopAdvertising(){
    	LibLog.d(DEBUG_TAG, "stopAdvertising");
        Objects.requireNonNull(AdvertiserManager.getInstance()).stopAdvertising();
        Objects.requireNonNull(ConnectionRequestReceiverManager.getInstance()).stopConnectionRequestReceiving();
    }

    public void startScanning(ConnectionManagerScanningCallBacks connectionManagerScanningCallBacks){
    	LibLog.d(DEBUG_TAG, "startScanning");
        this.connectionManagerScanningCallBacks = connectionManagerScanningCallBacks;
        Objects.requireNonNull(ScannerManager.getInstance()).startScanning();
    }

    public void stopScanning(){
    	LibLog.d(DEBUG_TAG, "stopScanning");
        Objects.requireNonNull(ScannerManager.getInstance()).stopScanning();
    }

    public void sendConnectionRequest(String myProfileName, DTDiscoveredClient discoveredClient, ConnectionManagerConnectionRequestSenderCallBacks connectionManagerConnectionRequestSenderCallBacks) {
        this.connectionManagerConnectionRequestSenderCallBacks = connectionManagerConnectionRequestSenderCallBacks;
        Objects.requireNonNull(ConnectionRequestSenderManager.getInstance()).sendConnectionRequest(myProfileName, discoveredClient);
    }

    private class AdvertiseManagerCallBackHandler implements AdvertiserManager.AdvertiserManagerCallBacks{

        @Override
        public void advertiserManagerStartedAdvertising() {
            connectionManagerAdvertisingCallBacks.connectionManagerStartedAdvertising();
        }

        @Override
        public void advertiserManagerStoppedAdvertising() {
            connectionManagerAdvertisingCallBacks.connectionManagerStoppedAdvertising();
        }

    }

    private class ScannerManagerCallBackHandler implements ScannerManager.ScannerManagerCallBacks{

        @Override
        public void scannerManagerStartedScanning() {
            connectionManagerScanningCallBacks.connectionManagerStartedScanning();
        }

        @Override
        public void scannerManagerStoppedScanning() {
            connectionManagerScanningCallBacks.connectionManagerStoppedScanning();
        }

        @Override
        public void scannerManagerDiscoveredClients(DTDiscoveredClient discoveredClient) {
            connectionManagerScanningCallBacks.connectionManagerDiscoveredClients(discoveredClient);
        }

        @Override
        public void scannerManagerFoundError(DTErrors.ScanningError scanningError) {
            connectionManagerScanningCallBacks.connectionManagerFoundError(scanningError);
        }
    }

    private class ConnectionRequestSenderManagerCallBackHandler implements ConnectionRequestSenderManager.ConnectorManagerCallBacks{

        @Override
        public void connectionManagerOnSuccessfulConnected(DTClient connectedClient) {
            connectionManagerConnectionRequestSenderCallBacks.connectionManagerSuccessfullyConnected(connectedClient);
        }

        @Override
        public void connectionManagerOnConnectionFailed(String message) {
        	connectionManagerConnectionRequestSenderCallBacks.connectionManagerFailedToConnect(message);
        }
    }
    
    private class ConnectionRequestReceiverManagerCallBackHandler implements ConnectionRequestReceiverManagerCallBacks{

		@Override
		public void connectionRequestReceiverStartedReceiving(int port, String ipAddress) {
			//do nothing
		}

		@Override
		public void connectionRequestReceiverStoppedReceiving() {
			//do nothing
		}

		@Override
		public void connectionRequestReceiverGotConnectedWithClient(DTClient connectedClient) {
			connectionManagerConnectionRequestReceiverCallBacks.connectionManagerGotConnectedWithClient(connectedClient);
		}
    	
    }

    public interface ConnectionManagerAdvertisingCallBacks{
        void connectionManagerStartedAdvertising();
        void connectionManagerStoppedAdvertising();
        void connectionManagerGotConnected(DTClient connectedClient);
    }

    public interface ConnectionManagerScanningCallBacks{
        void connectionManagerStartedScanning();
        void connectionManagerStoppedScanning();
        void connectionManagerDiscoveredClients(DTDiscoveredClient discoveredClient);
        void connectionManagerFoundError(DTErrors.ScanningError scanningError);
    }

    public interface ConnectionManagerConnectionRequestSenderCallBacks{
        void connectionManagerSuccessfullyConnected(DTClient connectedClient);
        void connectionManagerFailedToConnect(String message);
    }
    
    public interface ConnectionManagerConnectionRequestReceiverCallBacks{
    	void connectionManagerStartedConnectionRequestReceiving(int port, String ipAddress);
        void connectionManagerStoppedConnectionRequestReceiving();
        void connectionManagerGotConnectedWithClient(DTClient connectedClient);
    }

}
