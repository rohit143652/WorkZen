package com.example.application.employee_module.dto;

import java.util.List;

/** The COMPLETE set of additional permissions this employee should have (not a delta). Permissions the editor isn't allowed to touch are kept as they are regardless of what is sent. */
public record ExtraPermissionsRequest(List<Long> permissionIds) {
}
