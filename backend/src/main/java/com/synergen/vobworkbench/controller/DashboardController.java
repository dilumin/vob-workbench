package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.AdminDtos.DashboardSummary;
import com.synergen.vobworkbench.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final AdminService adminService;

    public DashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return adminService.dashboard();
    }
}
