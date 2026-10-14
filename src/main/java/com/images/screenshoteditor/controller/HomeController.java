package com.images.screenshoteditor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

//A controller receives web requests and decides what response to return.

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
