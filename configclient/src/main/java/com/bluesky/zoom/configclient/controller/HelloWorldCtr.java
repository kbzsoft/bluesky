package com.bluesky.zoom.configclient.controller;

import com.bluesky.zoom.configclient.po.User;
import com.bluesky.zoom.configclient.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldCtr {
    @Autowired
    private UserService userService;

    @RequestMapping("/hello")
    public String index() {
        long beginTime=System.currentTimeMillis();
        User user = userService.selectByPrimaryKey(1);
        long time=System.currentTimeMillis()-beginTime;
        return "Hello SpringBoot"+user.getName()+",消耗查询时间："+time;

    }
}
