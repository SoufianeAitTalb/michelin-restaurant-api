package fr.fst.michelin.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// les roles sont dans resource_access.coursm2.roles du token
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLIENT_ID = "coursm2";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || resourceAccess.get(CLIENT_ID) == null) {
            return authorities;
        }

        Map<String, Object> client = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
        List<String> roles = (List<String>) client.get("roles");
        if (roles == null) {
            return authorities;
        }

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }
}
