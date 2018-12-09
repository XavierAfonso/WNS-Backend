package ch.heigvd.wns.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;

public class MyUserPrincipal extends User {
    private User user;

    public MyUserPrincipal(Long id, String username, String password, String email, Collection<? extends GrantedAuthority> authorities, @SuppressWarnings("unused") boolean enabled, LocalDate lastPasswordResetDate, String userSecret) {
        super(username, password, enabled, true, true, true, authorities);
    }

    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    //...
}