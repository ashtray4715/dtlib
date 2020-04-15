package com.gobinda;

public class DTDiscoveredClient {

    private String name;
    private String ipAddress;

    public DTDiscoveredClient(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

}
