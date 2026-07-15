package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.PatientDtos.PatientPatch;
import com.synergen.vobworkbench.dto.PatientDtos.PatientResponse;
import com.synergen.vobworkbench.dto.PatientDtos.PatientWrite;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.security.SecurityUtils;
import com.synergen.vobworkbench.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;
    private final SecurityUtils securityUtils;

    public PatientController(PatientService patientService, SecurityUtils securityUtils) {
        this.patientService = patientService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public PageResponse<PatientResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return patientService.search(q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPERVISOR_ADMIN')")
    public PatientResponse create(@Valid @RequestBody PatientWrite request) {
        securityUtils.requireAny(Role.RECEPTIONIST, Role.SUPERVISOR_ADMIN);
        return patientService.create(request);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable String id) {
        return patientService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPERVISOR_ADMIN')")
    public PatientResponse patch(@PathVariable String id, @Valid @RequestBody PatientPatch request) {
        securityUtils.requireAny(Role.RECEPTIONIST, Role.SUPERVISOR_ADMIN);
        return patientService.patch(id, request);
    }
}
