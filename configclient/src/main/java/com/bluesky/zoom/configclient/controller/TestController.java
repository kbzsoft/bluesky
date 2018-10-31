package com.bluesky.zoom.configclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class TestController {
    @Value("${server.port}")
    private String profile;
    @GetMapping("/getProfile")
    public String hello(){
        return this.profile;
    }
}
