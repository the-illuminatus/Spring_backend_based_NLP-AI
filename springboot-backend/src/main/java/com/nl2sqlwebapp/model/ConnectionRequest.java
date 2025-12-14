package com.nl2sqlwebapp.model;

import lombok.Data;

@Data
public class ConnectionRequest {
    private String host;
    private String user;
    private String password;
    private String database;
    private String port = "3306";
}

