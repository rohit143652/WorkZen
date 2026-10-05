package com.example.application.employee_module.dto;

import java.util.List;

/**
 * An employee's access split into its two sources, for the "additional permissions" editor.
 *  - {@code rolePermissions}: what their role(s) already give (read-only context).
 *  - {@code extraPermissions}: what was granted to THIS employee on top of the role.
 *  - {@code grantable}: what the person editing may toggle - their own permissions, not already
 *    given by the role - plus any current extra they can't change (flagged {@code locked}).
 */
public record ExtraPermissionsResponse(Long employeeId, String username, List<String> roleNames,
                                       List<PermissionOption> rolePermissions,
                                       List<PermissionOption> extraPermissions,
                                       List<PermissionOption> grantable) {
}
