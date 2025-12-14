package com.nl2sqlwebapp.controller;

import com.nl2sqlwebapp.model.*;
import com.nl2sqlwebapp.service.Nl2SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class Nl2SqlController {

    @Autowired
    private Nl2SqlService service;

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("message", "NL2SQL Spring Boot Web Backend is running");
    }

    @PostMapping("/connect")
    public ConnectionResponse connect(@RequestBody ConnectionRequest req) {
        return service.connect(req);
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest req) {
        return service.executeQuery(req);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.getStatus();
    }
}

