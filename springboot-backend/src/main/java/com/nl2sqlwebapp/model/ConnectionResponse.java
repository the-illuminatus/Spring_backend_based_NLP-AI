package com.nl2sqlwebapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionResponse {
    private boolean success;
    private String message;
}

