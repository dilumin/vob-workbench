package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.AdminDtos.UserCreate;
import com.synergen.vobworkbench.dto.AuthDtos.UserResponse;
import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.listUsers(page, size);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreate request) {
        return adminService.createUser(request);
    }

    @GetMapping("/mock-data")
    public MockData mockData() {
        return adminService.getMockData();
    }

    @PutMapping("/mock-data")
    public MockData saveMockData(@RequestBody MockData request) {
        return adminService.saveMockData(request);
    }
}
