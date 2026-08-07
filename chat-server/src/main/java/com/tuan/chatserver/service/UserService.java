package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.Person;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final RefreshTokenService refreshTokenService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${app.base-url}")
    private String baseUrl;
    private static final String VERIFICATION_URL = "/api/auth/verify?token=";

    @Autowired
    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder bCryptPasswordEncoder,
                       VerificationService verificationService,
                       EmailService emailService,
                       EmailTemplateService emailTemplateService,
                       AuthService authService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.verificationService=verificationService;
        this.emailService=emailService;
        this.emailTemplateService=emailTemplateService;
        this.authService=authService;
        this.refreshTokenService=refreshTokenService;
    }

    @Transactional
    public void register(UserRegisterRequest userRegisterRequest){
        String fullname = userRegisterRequest.getFullname();
        String username = userRegisterRequest.getUsername();
        String email = userRegisterRequest.getEmail();
        String password = userRegisterRequest.getPassword();
        String phone = userRegisterRequest.getPhone();
        logger.info("Register attempt for username: {}, email: {}", username, email);

        if(userRepository.existsByUsername(username) || userRepository.existsByEmail(email)){
            logger.warn("Register failed - username or email already exists: username={}, email={}", username, email);
            throw new UsernameOrEmailAlreadyExistsException();
        }

        String newVerificationToken=UUID.randomUUID().toString();
        String hashedPassword = bCryptPasswordEncoder.encode(password);
        PendingRegistration pendingRegistration=new PendingRegistration(email, username, hashedPassword, fullname, phone);

        verificationService.createVerification(newVerificationToken, pendingRegistration);

        String verificationUrl=baseUrl+VERIFICATION_URL+newVerificationToken;

        String htmlContent=emailTemplateService.buildVerificationEmail(username, verificationUrl);
        emailService.sendHtmlMail(email, "Verify your Chat Server account", htmlContent);
    }

    @Transactional
    public void verifyRegistration(String token){
        PendingRegistration pendingRegistration=verificationService.getPendingRegistration(token).orElseThrow(() -> {
            throw new InvalidVerificationTokenException(token);
        });

        String fullname=pendingRegistration.getFullname();
        String username=pendingRegistration.getUsername();
        String email=pendingRegistration.getEmail();
        String password=pendingRegistration.getHashedPassword();
        String phone=pendingRegistration.getPhone();

        verificationService.removeVerification(token, email);

        User user=new User(fullname, username, email, password, phone, true);
        try{
            userRepository.save(user);
        }catch(Exception e){
            throw new DataAccessFailureException(e);
        }
    }

    @Transactional
    public void deleteAccount(Authentication authentication, String password){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Person person = userDetails.getPerson();
        Long id=person.getId();

        logger.info("Delete account attempt for userId: {}", id);

        User actualUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (!bCryptPasswordEncoder.matches(password, actualUser.getPassword())) {
            throw new WrongPasswordOrInactiveAccountException();
        }

        refreshTokenService.findTokenByPersonId(id)
                .ifPresent(refreshTokenService::revoke);

        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Delete account failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        actualUser.setActive(false);
        try{
            userRepository.save(actualUser);
            logger.info("Delete account successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Delete account failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    public MyProfileDTO getProfile(Long id){
        logger.debug("Fetching profile for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Get profile failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }
    }

    public void updateProfile(Long id, UpdateProfileRequest updateProfileRequest){
        String fullname = updateProfileRequest.getFullname();
        String username = updateProfileRequest.getUsername();
        String email = updateProfileRequest.getEmail();
        String phone = updateProfileRequest.getPhone();
        logger.info("Update profile attempt for userId: {}", id);

        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Update profile failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        if(!actualUser.isActive()){
            logger.warn("Update profile failed - account inactive for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        boolean usernameAvailable = actualUser.getUsername().equals(username) || !userRepository.existsByUsername(username);
        boolean emailAvailable = actualUser.getEmail().equals(email) || !userRepository.existsByEmail(email);
        if(!usernameAvailable || !emailAvailable){
            logger.warn("Update profile failed - username or email already taken for userId: {}", id);
            throw new UsernameOrEmailAlreadyExistsException();
        }

        actualUser.setFullname(fullname);
        actualUser.setUsername(username);
        actualUser.setEmail(email);
        actualUser.setPhone(phone);
        try{
            userRepository.save(actualUser);
            logger.info("Update profile successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Update profile failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void changePassword(Long id, ChangePasswordRequest changePasswordRequest){
        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        logger.info("Change password attempt for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Change password failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        if(!actualUser.isActive()){
            logger.warn("Change password failed - account inactive for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        if(!bCryptPasswordEncoder.matches(oldPassword, actualUser.getPassword())){
            logger.warn("Change password failed - old password mismatch for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        try{
            actualUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userRepository.save(actualUser);
            logger.info("Change password successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Change password failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    private OtherProfileDTO findActiveUserByUsername(String username){
        logger.debug("Searching active user by exact username: {}", username);

        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> {
                    logger.warn("Search failed - no active user found for username: {}", username);
                    return new UserNotFoundException(username);
                });

        return UserMapper.mapUserToOtherUserDTO(user);
    }

    private OtherProfileDTO findActiveUserByEmail(String email){
        logger.debug("Searching active user by exact email: {}", email);

        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> {
                    logger.warn("Search failed - no active user found for email: {}", email);
                    return new UserNotFoundException(email);
                });

        return UserMapper.mapUserToOtherUserDTO(user);
    }

    public OtherProfileDTO findActiveUserByUsernameOrEmail(String identifier){
        return identifier.contains("@")
                ? findActiveUserByEmail(identifier)
                : findActiveUserByUsername(identifier);
    }
}