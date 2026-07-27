package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.RefreshToken;
import com.tuan.chatserver.enums.UserRole;
import com.tuan.chatserver.exception.InvalidRefreshTokenException;
import com.tuan.chatserver.repository.PersonRepository;
import com.tuan.chatserver.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PersonRepository personRepository;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, PersonRepository personRepository){
        this.authenticationManager=authenticationManager;
        this.jwtService=jwtService;
        this.refreshTokenService=refreshTokenService;
        this.personRepository=personRepository;
    }

    public LoginResponse login(LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword()));
        CustomUserDetails userDetails= (CustomUserDetails) authentication.getPrincipal();
        Person person = userDetails.getPerson();
        UserRole role = (person instanceof Admin) ? UserRole.ADMIN : UserRole.USER;
        String accessToken = jwtService.generateAccessToken(person.getId(), person.getUsername(), role, person.getTokenVersion());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(person);
        PersonDTO personDTO=new PersonDTO(person.getId(), person.getUsername(), person.getRole(), person.isActive());
        return new LoginResponse(personDTO, accessToken, refreshToken.getToken());
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest){
        String requestRefreshToken = refreshTokenRequest.getRefreshToken();
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        refreshTokenService.validateToken(refreshToken);

        Person person = refreshToken.getPerson();
        String newAccessToken = jwtService.generateAccessToken(person.getId(), person.getUsername(), person.getRole(), person.getTokenVersion());

        return new RefreshTokenResponse(newAccessToken, refreshToken.getToken());
    }

    public void logout(Authentication authentication, LogoutRequest logoutRequest){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Person person = userDetails.getPerson();
        String token=logoutRequest.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(token)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!refreshToken.getPerson().getId().equals(person.getId())) {
            throw new InvalidRefreshTokenException();
        }

        person.setTokenVersion(person.getTokenVersion() + 1);
        personRepository.save(person);

        refreshTokenService.revoke(token);
    }
}
