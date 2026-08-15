package com.truthlens.api.service;

import com.truthlens.api.model.User;
import com.truthlens.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));

        boolean isNotBanned = !"BANNED".equalsIgnoreCase(user.getStatus());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isNotBanned, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                isNotBanned, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
