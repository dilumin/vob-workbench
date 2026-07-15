package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.dto.CommonDtos.PageInfo;
import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.AdminDtos.DashboardSummary;
import com.synergen.vobworkbench.dto.AdminDtos.UserCreate;
import com.synergen.vobworkbench.dto.AuthDtos.UserResponse;
import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.MockData;
import com.synergen.vobworkbench.model.Priority;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.model.VobRequest;
import com.synergen.vobworkbench.model.VobStatus;
import com.synergen.vobworkbench.repository.MockDataRepository;
import com.synergen.vobworkbench.repository.UserRepository;
import com.synergen.vobworkbench.repository.VobRequestRepository;
import com.synergen.vobworkbench.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final UserRepository users;
    private final VobRequestRepository requests;
    private final MockDataRepository mockData;
    private final PasswordEncoder encoder;
    private final SecurityUtils securityUtils;
    private final AuthService authService;
    private final AuditService auditService;

    public AdminService(UserRepository users, VobRequestRepository requests, MockDataRepository mockData,
                        PasswordEncoder encoder, SecurityUtils securityUtils, AuthService authService,
                        AuditService auditService) {
        this.users = users;
        this.requests = requests;
        this.mockData = mockData;
        this.encoder = encoder;
        this.securityUtils = securityUtils;
        this.authService = authService;
        this.auditService = auditService;
    }

    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public PageResponse<UserResponse> listUsers(int page, int size) {
        securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
        List<UserResponse> content = users.findAll().stream().map(authService::toResponse).toList();
        int from = Math.min(page * size, content.size());
        int to = Math.min(from + size, content.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) content.size() / size);
        return new PageResponse<>(content.subList(from, to), new PageInfo(page, size, content.size(), totalPages));
    }

    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public UserResponse createUser(UserCreate request) {
        securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
        if (users.existsByUsername(request.username())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "A user with this username already exists.");
        }
        Instant now = Instant.now();
        User user = new User(null, request.username(), encoder.encode(request.password()), request.fullName(), request.role(), true, now, now);
        User saved = users.save(user);
        auditService.logUserRegistered(saved);
        return authService.toResponse(saved);
    }

    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public MockData getMockData() {
        securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
        return mockData.findById("default").orElse(new MockData());
    }

    @PreAuthorize("hasRole('SUPERVISOR_ADMIN')")
    public MockData saveMockData(MockData request) {
        securityUtils.requireAny(Role.SUPERVISOR_ADMIN);
        request.setId("default");
        MockData saved = mockData.save(request);
        auditService.logMockDataUpdated();
        return saved;
    }

    @PreAuthorize("hasAnyRole('VIEWER', 'RECEPTIONIST', 'SPECIALIST', 'SUPERVISOR_ADMIN')")
    public DashboardSummary dashboard() {
        securityUtils.requireAny(Role.VIEWER, Role.RECEPTIONIST, Role.SPECIALIST, Role.SUPERVISOR_ADMIN);
        List<VobRequest> all = requests.findAll();
        return new DashboardSummary(
                all.size(),
                countStatus(all, VobStatus.PENDING),
                countStatus(all, VobStatus.IN_PROGRESS),
                countStatus(all, VobStatus.VERIFIED),
                all.stream().filter(request -> request.getPriority() == Priority.URGENT).count()
        );
    }

    private long countStatus(List<VobRequest> requests, VobStatus status) {
        return requests.stream().filter(request -> request.getStatus() == status).count();
    }
}
