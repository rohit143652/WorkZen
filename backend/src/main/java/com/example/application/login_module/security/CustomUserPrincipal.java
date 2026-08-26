package com.example.application.login_module.security;

import com.example.application.login_module.entity.User;
import com.example.application.permission_module.entity.Permission;
import com.example.application.role_module.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security principal backed by the database User entity.
 * Authorities include both ROLE_x (for hasRole) and raw permission
 * names (for hasAuthority), all derived from the database.
 */
public class CustomUserPrincipal implements UserDetails {

    private final User user;

    public CustomUserPrincipal(User user) {
        this.user = user;
    }

    public Long getId() { return user.getId(); }

    public Long getClientCompanyId() { return user.getClientCompanyId(); }

    public User getUser() { return user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
        return authorities;
    }

    public Set<String> getRoleNames() {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    public Set<String> getPermissionNames() {
        Set<String> permissions = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getName());
            }
        }
        return permissions;
    }

    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !user.isLocked(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.isActive(); }
}
