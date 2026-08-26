package com.example.application.permission_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.permission_module.dto.PermissionRequest;
import com.example.application.permission_module.dto.PermissionResponse;
import com.example.application.permission_module.entity.Permission;
import com.example.application.permission_module.repository.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public PermissionService(PermissionRepository permissionRepository, AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public PermissionResponse create(PermissionRequest request, Long actorId, HttpServletRequest httpRequest) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Permission already exists: " + request.getName());
        }
        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        Permission saved = permissionRepository.save(permission);
        auditService.log(actorId, "PERMISSION_UPDATED", "Created permission " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request, Long actorId, HttpServletRequest httpRequest) {
        Permission permission = getEntity(id);
        permission.setDescription(request.getDescription());
        if (!permission.getName().equals(request.getName())) {
            if (permissionRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Permission already exists: " + request.getName());
            }
            permission.setName(request.getName());
        }
        Permission saved = permissionRepository.save(permission);
        auditService.log(actorId, "PERMISSION_UPDATED", "Updated permission " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        Permission permission = getEntity(id);
        permissionRepository.delete(permission);
        auditService.log(actorId, "PERMISSION_UPDATED", "Deleted permission " + permission.getName(), httpRequest);
    }

    private Permission getEntity(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }

    private PermissionResponse toResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getDescription(), p.isActive());
    }
}
