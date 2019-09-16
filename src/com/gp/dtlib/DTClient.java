package com.gp.dtlib;

import java.net.Socket;

public class DTClient extends DTDiscoveredClient{
    private Socket clientSocket;

    public DTClient(String name, String ipAddress, Socket clientSocket) {
        super(name,ipAddress);
        this.clientSocket = clientSocket;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
