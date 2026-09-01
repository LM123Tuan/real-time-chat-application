package com.tuan.chatserver.service;

import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.UserNotFoundException;
import com.tuan.chatserver.repository.AdminRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, AdminRepository adminRepository){
        this.userRepository=userRepository;
        this.adminRepository=adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier){
        if(identifier.contains("@")){
            Optional<User> user = userRepository.findByEmail(identifier);
            if (user.isPresent()) {
                return new CustomUserDetails(user.get());
            }
            throw new UsernameNotFoundException(identifier);
        }else{
            Optional<User> user = userRepository.findByUsername(identifier);
            if (user.isPresent()) {
                return new CustomUserDetails(user.get());
            }
            Optional<Admin> admin = adminRepository.findByUsername(identifier);
            if (admin.isPresent()) {
                return new CustomUserDetails(admin.get());
            }
            throw new UsernameNotFoundException(identifier);
        }
    }

    public UserDetails loadUserById(Long id){
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return new CustomUserDetails(user.get());
        }
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            return new CustomUserDetails(admin.get());
        }
        throw new UserNotFoundException(id);
    }
}