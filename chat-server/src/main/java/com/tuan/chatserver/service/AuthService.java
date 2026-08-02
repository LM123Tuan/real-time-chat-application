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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ResetPasswordService resetPasswordService;
    private final EmailTemplateService emailTemplateService;
    private final EmailService emailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PersonRepository personRepository,
            UserRepository userRepository,
            RedisService redisService,
            ResetPasswordService resetPasswordService,
            EmailTemplateService emailTemplateService,
            EmailService emailService,
            BCryptPasswordEncoder bCryptPasswordEncoder
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
        Person person = userDetails.getPerson();

        person.setTokenVersion(person.getTokenVersion() + 1);
        personRepository.save(person);

        UserRole role = (person instanceof Admin) ? UserRole.ADMIN : UserRole.USER;
        String accessToken = jwtService.generateAccessToken(person.getId(), person.getUsername(), role, person.getTokenVersion());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(person);
        PersonDTO personDTO=new PersonDTO(person.getId(), person.getUsername(), person.getRole(), person.isActive());
        return new LoginResponse(personDTO, accessToken, refreshToken.getToken());
    }

    @Transactional
    public String loginWithGoogle(OidcUser oidcUser){
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String fullname = oidcUser.getFullName();
        String username;
        do {
            username = "User" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8);
        } while (userRepository.existsByUsername(username));
        //String avatar = oidcUser.getPicture();

        Optional<User> optionalUser=userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, googleId);
        User user;
        if(optionalUser.isPresent()){
            user=optionalUser.get();
        }else{
            Optional<User> checkUser=userRepository.findByEmail(email);
            if(checkUser.isPresent()){
                if (!checkUser.get().getProvider().equals(AuthProvider.GOOGLE)){
                    throw new UserNotFoundException(email);
                }
            }
            user = new User(fullname, username, email, null, null, true, AuthProvider.GOOGLE, googleId);
            userRepository.save(user);
        }

        user.setTokenVersion(user.getTokenVersion() + 1);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        PersonDTO personDTO=new PersonDTO(user.getId(), user.getUsername(), user.getRole(), user.isActive());
        LoginResponse loginResponse = new LoginResponse(personDTO, accessToken, refreshToken.getToken());

        String exchangeToken = UUID.randomUUID().toString();
        redisService.set("exchange:"+exchangeToken, loginResponse, EXCHANGE_TOKEN_TTL);
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

        Optional<String> oldToken=resetPasswordService.getTokenByEmail(email);
        if(oldToken.isPresent()){
            resetPasswordService.removeResetPassword(oldToken.get(), email);
        }

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