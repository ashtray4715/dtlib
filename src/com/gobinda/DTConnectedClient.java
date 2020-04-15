package com.gobinda;

import java.net.Socket;

public class DTConnectedClient extends DTDiscoveredClient {

    public enum ConnectionRole {
        SERVER,
        CLIENT
    }

    private Socket clientSocket;
    private ConnectionRole role;

    DTConnectedClient(String name, String ipAddress, Socket clientSocket, ConnectionRole role) {
        super(name,ipAddress);
        this.clientSocket = clientSocket;
        this.role = role;
    }

    public DTConnectedClient(DTDiscoveredClient discoveredClient, Socket clientSocket) {
        super(discoveredClient.getName(), discoveredClient.getIpAddress());
        this.clientSocket = clientSocket;
    }

    Socket getClientSocket() {
        return clientSocket;
    }

    public ConnectionRole getRole() {
        return role;
    }
}