package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.RefreshToken;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.AuthProvider;
import com.tuan.chatserver.enums.UserRole;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.repository.PersonRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${app.frontend-url}")
    private String frontendUrl;
    private static final Duration EXCHANGE_TOKEN_TTL = Duration.ofSeconds(30);
    private static final String RESET_PASSWORD_URL = "/api/auth/confirm-reset-password?token=";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ResetPasswordService resetPasswordService;
    private final EmailTemplateService emailTemplateService;
    private final EmailService emailService;
    private final PasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public AuthService(
            @Lazy AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PersonRepository personRepository,
            UserRepository userRepository,
            RedisService redisService,
            ResetPasswordService resetPasswordService,
            EmailTemplateService emailTemplateService,
            EmailService emailService,
            PasswordEncoder bCryptPasswordEncoder
    ){
        this.authenticationManager=authenticationManager;
        this.jwtService=jwtService;
        this.refreshTokenService=refreshTokenService;
        this.personRepository=personRepository;
        this.userRepository=userRepository;
        this.redisService=redisService;
        this.resetPasswordService=resetPasswordService;
        this.emailTemplateService=emailTemplateService;
        this.emailService=emailService;
        this.bCryptPasswordEncoder=bCryptPasswordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword()));
        CustomUserDetails userDetails= (CustomUserDetails) authentication.getPrincipal();
        Long id = userDetails.getPerson().getId();

        int updatedRows = personRepository.incrementTokenVersion(id);
        if (updatedRows == 0) {
            logger.warn("incrementTokenVersion affected 0 rows for userId={}", id);
        }

        Person updatedPerson = personRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        UserRole role = (updatedPerson instanceof Admin) ? UserRole.ADMIN : UserRole.USER;
        String accessToken = jwtService.generateAccessToken(updatedPerson.getId(), updatedPerson.getUsername(), role, updatedPerson.getTokenVersion());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(updatedPerson);
        PersonDTO personDTO=new PersonDTO(updatedPerson.getId(), updatedPerson.getUsername(), updatedPerson.getRole(), updatedPerson.isActive());
        return new LoginResponse(personDTO, accessToken, refreshToken.getToken());
    }

    @Transactional
    public String loginWithGoogle(OidcUser oidcUser) {
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String fullname = oidcUser.getFullName();
        String username;
        //String avatar = oidcUser.getPicture();

        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, googleId);
        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            Optional<User> checkUser = userRepository.findByEmail(email);
            if (checkUser.isPresent()) {
                if (!checkUser.get().getProvider().equals(AuthProvider.GOOGLE)) {
                    throw new UserNotFoundException(email);
                }
            }
            do {
                username = "User" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8);
            } while (userRepository.existsByUsername(username));
            user = new User(fullname, username, email, null, null, true, AuthProvider.GOOGLE, googleId);
            try {
                userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                logger.error("Unexpected username collision during Google login, username={}", username, e);
                throw new DataAccessFailureException(e);
            }
        }

        int updatedRows = personRepository.incrementTokenVersion(user.getId());
        if (updatedRows == 0) {
            logger.warn("incrementTokenVersion affected 0 rows for userId={}", user.getId());
        }

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(user.getId()));
        String accessToken = jwtService.generateAccessToken(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole(), updatedUser.getTokenVersion());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(updatedUser);
        PersonDTO personDTO = new PersonDTO(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole(), updatedUser.isActive());
        LoginResponse loginResponse = new LoginResponse(personDTO, accessToken, refreshToken.getToken());

        String exchangeToken = UUID.randomUUID().toString();
        redisService.set("exchange:" + exchangeToken, loginResponse, EXCHANGE_TOKEN_TTL);
        return exchangeToken;
    }

    public LoginResponse exchangeToken(String exchangeToken){
        LoginResponse loginResponse = redisService.getAndDelete("exchange:"+exchangeToken, LoginResponse.class).orElseThrow(() -> {
            throw new InvalidExchangeTokenException(exchangeToken);
        });
        return loginResponse;
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

    @Transactional
    public void initiateResetPassword(ForgotPasswordRequest forgotPasswordRequest){
        String email=forgotPasswordRequest.getEmail();

        User user=userRepository.findByEmail(email).orElseThrow(() -> {
            throw new UserNotFoundException(email);
        });
        if(!user.isActive()){
            throw new WrongPasswordOrInactiveAccountException();
        }

        String username=user.getUsername();

        String newToken=UUID.randomUUID().toString();
        resetPasswordService.createResetPassword(newToken, email);

        String resetPasswordUrl=baseUrl+RESET_PASSWORD_URL+newToken;
        String htmlContent=emailTemplateService.buildResetPasswordEmail(username, resetPasswordUrl);
        emailService.sendHtmlMail(email, "Reset password for your Chat Server account", htmlContent);
    }

    public String confirmResetPassword(String token){
        String email=resetPasswordService.getEmailByToken(token).orElseThrow(() -> {
            throw new InvalidResetPasswordTokenException(token);
        });

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();

        return redirectUrl;
    }

    @Transactional
    public void resetPassword(String token, ResetPasswordRequest resetPasswordRequest){
        String email=resetPasswordService.getEmailByToken(token).orElseThrow(() -> {
            throw new InvalidResetPasswordTokenException(token);
        });
        resetPasswordService.removeResetPassword(token, email);

        User user=userRepository.findByEmail(email).orElseThrow(() -> {
            throw new UserNotFoundException(email);
        });
        if(!user.isActive()){
            throw new WrongPasswordOrInactiveAccountException();
        }

        String newPassword=resetPasswordRequest.getNewPassword();
        String password=bCryptPasswordEncoder.encode(newPassword);
        user.setPassword(password);
        try{
            userRepository.save(user);
        }catch(Exception e){
            throw new DataAccessFailureException(e);
        }
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

        refreshTokenService.revoke(token);
    }
}