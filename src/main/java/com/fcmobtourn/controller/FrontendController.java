package com.fcmobtourn.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendController {

    @GetMapping(value = {"/", "/FC Mobile Tournament.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public Resource tournamentPage() {
        return new FileSystemResource("FC Mobile Tournament.html");
    }

    @GetMapping(value = "/admin.html", produces = MediaType.TEXT_HTML_VALUE)
    public Resource adminPage() {
        return new FileSystemResource("admin.html");
    }
}