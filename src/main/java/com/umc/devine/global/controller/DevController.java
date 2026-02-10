package com.umc.devine.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DevController {

    @GetMapping("/dev")
    public String dev() {
        return "forward:/dev/index.html";
    }
}
