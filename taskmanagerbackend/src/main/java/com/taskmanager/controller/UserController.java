package com.taskmanager.controller;

import com.taskmanager.model.User;
import com.taskmanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // returns id + name only (no email/password) - used to populate the
    // "assign task to developer" dropdown in the frontend
    @GetMapping("/developers")
    public List<Map<String, Object>> getDevelopers() {
        List<User> developers = userService.getAllDevelopers();
        return developers.stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getName()))
                .toList();
    }
}
