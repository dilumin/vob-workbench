package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.dto.CommonDtos.PageInfo;
import com.synergen.vobworkbench.dto.CommonDtos.PageResponse;
import com.synergen.vobworkbench.dto.PatientDtos.PatientPatch;
import com.synergen.vobworkbench.dto.PatientDtos.PatientResponse;
import com.synergen.vobworkbench.dto.PatientDtos.PatientWrite;
import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.Patient;
import com.synergen.vobworkbench.repository.PatientRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PatientService {
    private final PatientRepository patients;
    private final AuditService auditService;

    public PatientService(PatientRepository patients, AuditService auditService) {
        this.patients = patients;
        this.auditService = auditService;
    }

    public PageResponse<PatientResponse> search(String query, int page, int size) {
        List<PatientResponse> filtered = patients.findAll().stream()
                .filter(patient -> matches(patient, query))
                .map(this::toResponse)
                .toList();
        auditService.logPatientSearched(page, size, StringUtils.hasText(query));
        return page(filtered, page, size);
    }

    public PatientResponse create(PatientWrite request) {
        if (patients.existsByMrn(request.mrn())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_MRN", "A patient with this MRN already exists.");
        }
        Instant now = Instant.now();
        Patient patient = new Patient(null, request.mrn(), request.firstName(), request.lastName(), request.dateOfBirth(),
                request.gender(), request.phone(), now, now);
        Patient saved = patients.save(patient);
        auditService.logPatientCreated(saved.getId());
        return toResponse(saved);
    }

    public PatientResponse get(String id) {
        Patient patient = find(id);
        auditService.logPatientViewed(patient.getId());
        return toResponse(patient);
    }

    public PatientResponse patch(String id, PatientPatch request) {
        Patient patient = find(id);
        List<String> changedFields = new ArrayList<>();
        if (request.firstName() != null && !Objects.equals(patient.getFirstName(), request.firstName())) {
            patient.setFirstName(request.firstName());
            changedFields.add("firstName");
        }
        if (request.lastName() != null && !Objects.equals(patient.getLastName(), request.lastName())) {
            patient.setLastName(request.lastName());
            changedFields.add("lastName");
        }
        if (request.dateOfBirth() != null && !Objects.equals(patient.getDateOfBirth(), request.dateOfBirth())) {
            patient.setDateOfBirth(request.dateOfBirth());
            changedFields.add("dateOfBirth");
        }
        if (request.gender() != null && !Objects.equals(patient.getGender(), request.gender())) {
            patient.setGender(request.gender());
            changedFields.add("gender");
        }
        if (request.phone() != null && !Objects.equals(patient.getPhone(), request.phone())) {
            patient.setPhone(request.phone());
            changedFields.add("phone");
        }
        patient.setUpdatedAt(Instant.now());
        Patient saved = patients.save(patient);
        auditService.logPatientUpdated(saved.getId(), changedFields);
        return toResponse(saved);
    }

    Patient find(String id) {
        return patients.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Patient was not found."));
    }

    private boolean matches(Patient patient, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String normalized = query.toLowerCase();
        return contains(patient.getMrn(), normalized)
                || contains(patient.getFirstName(), normalized)
                || contains(patient.getLastName(), normalized)
                || contains(patient.getPhone(), normalized);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getMrn(), patient.getFirstName(), patient.getLastName(),
                patient.getDateOfBirth(), patient.getGender(), patient.getPhone(), patient.getCreatedAt(), patient.getUpdatedAt());
    }

    private PageResponse<PatientResponse> page(List<PatientResponse> content, int page, int size) {
        int from = Math.min(page * size, content.size());
        int to = Math.min(from + size, content.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) content.size() / size);
        return new PageResponse<>(content.subList(from, to), new PageInfo(page, size, content.size(), totalPages));
    }
}
