package com.example.application.employee_module.dto;

/** One permission as shown in the "additional permissions" editor. {@code locked}: it is currently granted but the person editing does not hold it themselves, so they can neither add nor remove it. */
public record PermissionOption(Long id, String name, String description, boolean locked) {
}
