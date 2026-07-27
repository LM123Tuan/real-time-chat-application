package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

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

        String hashedPassword = bCryptPasswordEncoder.encode(password);
        User user = new User(fullname, username, email, hashedPassword, phone, true);
        try{
            userRepository.save(user);
            logger.info("Register successful for username: {}", username);
        }catch(Exception e){
            logger.error("Register failed while saving user: username={}", username, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void deleteAccount(Long id){
        logger.info("Delete account attempt for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Delete account failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        actualUser.setActive(false);
        try{
            userRepository.save(actualUser);
            logger.info("Delete account successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Delete account failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    public UserDTO getProfile(Long id){
        logger.debug("Fetching profile for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Get profile failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }
    }

    public void updateProfile(UpdateProfileRequest updateProfileRequest){
        Long id = updateProfileRequest.getId();
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

    public void changePassword(ChangePasswordRequest changePasswordRequest){
        Long id = changePasswordRequest.getId();
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

    public UserDTO findActiveUserByUsername(String username){
        logger.debug("Searching active user by exact username: {}", username);
        Optional<User> user = userRepository.findByUsernameAndIsActiveTrue(username);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Search failed - no active user found for username: {}", username);
            throw new UserNotFoundException(username);
        }
    }

    public UserDTO findActiveUserByEmail(String email){
        logger.debug("Searching active user by exact email: {}", email);
        Optional<User> user = userRepository.findByEmailAndIsActiveTrue(email);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Search failed - no active user found for email: {}", email);
            throw new UserNotFoundException(email);
        }
    }

    public UserDTO findActiveUserById(Long id){
        logger.debug("Searching active user by exact id: {}", id);
        Optional<User> user = userRepository.findByIdAndIsActiveTrue(id);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Search failed - no active user found for id: {}", id);
            throw new UserNotFoundException(id);
        }
    }
}