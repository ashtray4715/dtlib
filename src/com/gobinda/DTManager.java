package com.gobinda;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;

public class DTManager {
	private static final String DEBUG_TAG = "DTManager";

	private static DTManager dtManager;

	static {
		dtManager = new DTManager();
	}

	public static DTManager getInstance() {
		return dtManager;
	}

	private DTManagerAdvertiserCallBack dtManagerAdvertiserCallBack;
	private DTManagerScannerCallBack dtManagerScannerCallBack;
	private DTManagerConnCallBack dtManagerConnCallBack;
	private DTManagerDataCallBack dtManagerDataCallBack;

	private DTConnectedClient currentlyConnectedClient;

	private DTAdvertiser dtAdvertiser;
	private DTScanner dtScanner;
	private DTConnector dtConnector;

	public interface DTManagerConnCallBack {
		void onDisconnectDevice();

		void onConnectDevice();

		void onConnectionFailed();
	}

	public interface DTManagerAdvertiserCallBack {
		void onStarted();

		void onStopped();

		void onStartFailed();

		void onStoppedFailed();
	}

	public interface DTManagerScannerCallBack {
		void onStarted();

		void onStopped();

		void onStartFailed();

		void onStoppedFailed();

		void onDiscover(DTDiscoveredClient discoveredClient);
	}

	public interface DTManagerDataCallBack {
		void onSendSuccessful();

		void onSendFailed();

		void onReceiveSuccessful(Map<String, String> receivedData);
	}
	
	public interface DTManagerLog {
		void d(String message);
	}

	public void setDtManagerConnCallBack(DTManagerConnCallBack dtManagerConnCallBack) {
		this.dtManagerConnCallBack = dtManagerConnCallBack;
	}

	public void setDtManagerAdvertiserCallBack(DTManagerAdvertiserCallBack dtManagerAdvertiserCallBack) {
		this.dtManagerAdvertiserCallBack = dtManagerAdvertiserCallBack;
	}

	public void setDtManagerScannerCallBack(DTManagerScannerCallBack dtManagerScannerCallBack) {
		this.dtManagerScannerCallBack = dtManagerScannerCallBack;
	}

	public void setDtManagerDataCallBack(DTManagerDataCallBack dtManagerDataCallBack) {
		this.dtManagerDataCallBack = dtManagerDataCallBack;
	}
	
	public void setDTLogHandler(DTManagerLog dtManagerLog) {
		DTLog.setLogHandler(dtManagerLog);
	}
	
	public DTConnectedClient getConnectedClient() {
		return currentlyConnectedClient;
	}

	// MARK - SCANNING PART

	public void startScanning(String myProfileName) {
		DTLog.d(DEBUG_TAG, "called -> startScanning");
		dtScanner = new DTScanner(new DTScannerCallBackHandler(), myProfileName);
		dtScanner.startScanning();
	}

	public void stopScanning() {
		DTLog.d(DEBUG_TAG, "called -> stopScanning");
		if (dtScanner != null) {
			dtScanner.stopScanning();
		}
	}

	// MARK - ADVERTISING PART

	public void startAdvertising(String myProfileName) {
		DTLog.d(DEBUG_TAG, "called -> startAdvertising");
		dtAdvertiser = new DTAdvertiser(new DTAdvertiserCallBackHandler(), myProfileName);
		dtAdvertiser.startAdvertising();
	}

	public void stopAdvertising() {
		DTLog.d(DEBUG_TAG, "called -> stopAdvertising");
		if (dtAdvertiser != null) {
			dtAdvertiser.stopAdvertising();
		}
	}

	// MARK - CONNECT/DISCONNECT PART

	public void connect(DTDiscoveredClient discoveredClient, String myProfileName) {
		DTLog.d(DEBUG_TAG, "called -> connect");
		dtConnector = new DTConnector(new DTConnectorCallBackHandler(), myProfileName);
		dtConnector.sendConnectionRequest(discoveredClient);
	}

	public void disconnect() {
		DTLog.d(DEBUG_TAG, "called -> disconnect");
		dtConnector.disconnect(currentlyConnectedClient);
	}

	// MARK - SEND/RECEIVE DATA PART

	public void sendData(Map<String, String> sendInfo) {
		try {
			DataOutputStream dataOutputStream = new DataOutputStream(
					currentlyConnectedClient.getClientSocket().getOutputStream());
			dataOutputStream.writeUTF(DTUtils.mapToString(sendInfo));
			dataOutputStream.flush();
			DTLog.d(DEBUG_TAG, "sendData successful");
			if (dtManagerDataCallBack != null) {
				new Thread(() -> dtManagerDataCallBack.onSendSuccessful()).start();
			}
		} catch (Exception e) {
			DTLog.d(DEBUG_TAG, "sendData error");
		}
	}

	public class DTReceiver implements Runnable {
		@Override
		public void run() {
			DataInputStream dataInputStream = null;
			try {
				dataInputStream = new DataInputStream(currentlyConnectedClient.getClientSocket().getInputStream());
				String receivedData = null;
				while ((receivedData = dataInputStream.readUTF()) != null) {
					if (dtManagerDataCallBack != null) {
						String finalReceivedData = receivedData;
						new Thread(
								() -> dtManagerDataCallBack.onReceiveSuccessful(DTUtils.stringToMap(finalReceivedData)))
										.start();
					}
				}
			} catch (Exception e) {
				currentlyConnectedClient = null;
			} finally {
				DTLog.d(DEBUG_TAG, " DTReceiver ending, means -> client disconnect");
				if (dtManagerConnCallBack != null) {
					new Thread(() -> dtManagerConnCallBack.onDisconnectDevice()).start();
				}
			}
		}

	}

	private class DTAdvertiserCallBackHandler implements DTAdvertiser.DTAdvertiserCallBacks {

		@Override
		public void advertisingStarted(int port, String ipAddress) {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->advertisingStarted");
			if (dtManagerAdvertiserCallBack != null) {
				new Thread(() -> dtManagerAdvertiserCallBack.onStarted()).start();
			}
		}

		@Override
		public void advertisingStartedAlready() {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->advertisingStartedAlready");
			if (dtManagerAdvertiserCallBack != null) {
				new Thread(() -> dtManagerAdvertiserCallBack.onStartFailed()).start();
			}
		}

		@Override
		public void advertisingNotStated(String message) {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->advertisingNotStated");
			if (dtManagerAdvertiserCallBack != null) {
				new Thread(() -> dtManagerAdvertiserCallBack.onStartFailed()).start();
			}
		}

		@Override
		public void advertisingStopped() {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->advertisingStopped");
			if (dtManagerAdvertiserCallBack != null) {
				new Thread(() -> dtManagerAdvertiserCallBack.onStopped()).start();
			}
		}

		@Override
		public void advertisingStoppedAlready() {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->advertisingStoppedAlready");
			if (dtManagerAdvertiserCallBack != null) {
				new Thread(() -> dtManagerAdvertiserCallBack.onStoppedFailed()).start();
			}
		}

		@Override
		public void connectedWithClient(DTConnectedClient connectedClient) {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->connectedWithClient");
			currentlyConnectedClient = connectedClient;
			new Thread(new DTReceiver()).start();
			if (dtManagerConnCallBack != null) {
				new Thread(() -> dtManagerConnCallBack.onConnectDevice()).start();
			}
		}

		@Override
		public void connectionFailed() {
			DTLog.d(DEBUG_TAG, "DTAdvertiserCallBackHandler->connectionFailed");
			if (dtManagerConnCallBack != null) {
				new Thread(() -> dtManagerConnCallBack.onConnectionFailed()).start();
			}
		}
	}

	private class DTScannerCallBackHandler implements DTScanner.DTScannerCallBacks {

		@Override
		public void scanningStarted() {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->scanningStarted");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onStarted()).start();
			}
		}

		@Override
		public void scanningStartedAlready() {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->scanningStartedAlready");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onStartFailed()).start();
			}
		}

		@Override
		public void scanningNotStarted(String message) {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->scanningNotStarted");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onStartFailed()).start();
			}
		}

		@Override
		public void scanningStopped() {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->scanningStopped");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onStopped()).start();
			}
		}

		@Override
		public void scanningStoppedAlready() {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->scanningStoppedAlready");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onStoppedFailed()).start();
			}
		}

		@Override
		public void discoveredClient(DTDiscoveredClient discoveredClient) {
			DTLog.d(DEBUG_TAG, "DTScannerCallBackHandler->discoveredClient");
			if (dtManagerScannerCallBack != null) {
				new Thread(() -> dtManagerScannerCallBack.onDiscover(discoveredClient)).start();
			}
		}
	}

	private class DTConnectorCallBackHandler implements DTConnector.DTConnectorCallBacks {

		@Override
		public void connectionSuccessful(DTConnectedClient connectedClient) {
			DTLog.d(DEBUG_TAG, "DTConnectorCallBackHandler->connectionSuccessful");
			currentlyConnectedClient = connectedClient;
			new Thread(new DTReceiver()).start();
			if (dtManagerConnCallBack != null) {
				new Thread(() -> dtManagerConnCallBack.onConnectDevice()).start();
			} else {
			}
		}

		@Override
		public void connectionFailed(String message) {
			DTLog.d(DEBUG_TAG, "DTConnectorCallBackHandler->connectionFailed");
			if (dtManagerConnCallBack != null) {
				new Thread(() -> dtManagerConnCallBack.onConnectionFailed()).start();
			}
		}
	}
}
