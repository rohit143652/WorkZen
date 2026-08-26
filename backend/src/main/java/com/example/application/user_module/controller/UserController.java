package com.example.application.user_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.user_module.dto.*;
import com.example.application.user_module.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        UserResponse created = userService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("User created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody UserUpdateRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User updated",
                userService.update(id, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        userService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRoles(@PathVariable Long id,
                                                                   @Valid @RequestBody UserRolesRequest request,
                                                                   @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User roles updated",
                userService.updateRoles(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id,
                                                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User activated",
                userService.activate(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User deactivated",
                userService.deactivate(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> unlock(@PathVariable Long id,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User unlocked",
                userService.unlock(id, principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> resetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        String tempPassword = userService.resetPassword(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Temporary password issued. It will not be shown again.",
                java.util.Map.of("temporaryPassword", tempPassword)));
    }

    /** Admin-chosen password, as distinct from resetPassword's randomly-generated one. Same USER_UPDATE permission. */
    @PutMapping("/{id}/set-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminSetPasswordRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        userService.setPassword(id, request, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Password updated. The user will be required to change it on next login."));
    }
}
