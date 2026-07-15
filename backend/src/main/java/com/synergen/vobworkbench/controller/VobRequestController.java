package com.synergen.vobworkbench.controller;

import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestCreate;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestPatch;
import com.synergen.vobworkbench.dto.VobDtos.VobRequestResponse;
import com.synergen.vobworkbench.model.AuditEvent;
import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.VobStatus;
import com.synergen.vobworkbench.service.AuditService;
import com.synergen.vobworkbench.service.VobRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/vob-requests")
public class VobRequestController {
    private final VobRequestService vobRequestService;
    private final AuditService auditService;

    public VobRequestController(VobRequestService vobRequestService, AuditService auditService) {
        this.vobRequestService = vobRequestService;
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<VobRequestResponse> search(
            @RequestParam(required = false) VobStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return vobRequestService.search(status, assignedTo, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VobRequestResponse create(@Valid @RequestBody VobRequestCreate request) {
        return vobRequestService.create(request);
    }

    @GetMapping("/{id}")
    public VobRequestResponse get(@PathVariable String id) {
        return vobRequestService.get(id);
    }

    @PatchMapping("/{id}")
    public VobRequestResponse patch(@PathVariable String id, @Valid @RequestBody VobRequestPatch request) {
        return vobRequestService.patch(id, request);
    }

    @PostMapping("/{id}/eligibility-check")
    public VobRequestResponse runEligibility(@PathVariable String id) {
        return vobRequestService.runEligibility(id);
    }

    @PostMapping("/{id}/calculate-coverage")
    public CoverageSummary calculateCoverage(@PathVariable String id) {
        return vobRequestService.calculateCoverage(id);
    }

    @PostMapping("/{id}/verify")
    public VobRequestResponse verify(@PathVariable String id) {
        return vobRequestService.verify(id);
    }

    @PostMapping("/{id}/reopen")
    public VobRequestResponse reopen(@PathVariable String id) {
        return vobRequestService.reopen(id);
    }

    @GetMapping("/{id}/audit")
    public List<AuditEvent> audit(@PathVariable String id) {
        vobRequestService.ensureExists(id);
        return auditService.forRequest(id);
    }
}
