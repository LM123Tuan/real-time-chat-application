//TODO: Add some features
package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.EntityType;
import com.tuan.chatserver.enums.EventType;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.AdminMapper;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.AdminRepository;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.util.CursorCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import org.hibernate.PessimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ChatBoxRepository chatBoxRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursorCodec cursorCodec;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public AdminService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        ChatBoxRepository chatBoxRepository,
                        MessageRepository messageRepository,
                        PasswordEncoder passwordEncoder,
                        CursorCodec cursorCodec,
                        SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
        this.cursorCodec = cursorCodec;
        this.messagingTemplate = messagingTemplate;
    }

    //ADMIN

    public void registerAdmin(AdminRegisterRequest adminRegisterRequest) {
        String username = adminRegisterRequest.getUsername();
        String password = adminRegisterRequest.getPassword();
        logger.info("Attempting to register admin, username={}", username);
        if(adminRepository.existsByUsername(username)){
            logger.warn("Register admin failed: username already exists, username={}", username);
            throw new UsernameOrEmailAlreadyExistsException();
        }
        String hashedPassword = passwordEncoder.encode(password);
        Admin admin = new Admin(username, hashedPassword, true);
        try{
            adminRepository.save(admin);
            messagingTemplate.convertAndSend("/topic/admin",
                    new ChatEvent<>(EventType.ADMIN_REGISTERED, AdminMapper.mapAdminToAdminDTO(admin)));
            logger.info("Admin registered successfully, username={}", username);
        }catch (Exception e){
            logger.error("Error occurred while registering admin, username={}", username, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Cacheable(value = "adminByUsername", key = "#username")
    public AdminDTO findAdminByUsername(String username) {
        logger.debug("Fetching admin by username, username={}", username);
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin for username={}", username);
            return adminDTO;
        } else {
            logger.debug("No admin found for username={}", username);
            throw new UserNotFoundException(username);
        }
    }

    @Cacheable(value = "adminById", key = "#id")
    public AdminDTO findAdminById(Long id){
        logger.debug("Fetching admin by id, id={}", id);
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin for id={}", id);
            return adminDTO;
        } else {
            logger.debug("No admin found for id={}", id);
            throw new UserNotFoundException(id);
        }
    }

    @Cacheable(value = "adminById", key = "#id")
    public AdminDTO getAdminProfile(Long id){
        logger.debug("Fetching admin profile, id={}", id);
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin profile for id={}", id);
            return adminDTO;
        } else {
            logger.debug("No admin found for id={}", id);
            throw new UserNotFoundException(id);
        }
    }

    //USER

    @Cacheable(
            value = "admin_Users",
            key = "@cursorHelper.extractPageNumber(#request.cursor)",
            condition = "@cursorHelper.extractPageNumber(#request.cursor) < 5"
    )
    public CursorPaginationResponse<List<OtherProfileDTO>> findAllUsers(CursorPaginationRequest request) {
        logger.debug("Fetching all users");

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<User> users;
        long pageNumber=0;
        if (request.getCursor() == null) {
            users = userRepository.findAllOfFirstPage(pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            pageNumber=cursorData.getPageNumber();
            users = userRepository.findAllOfNextPage(cursorData.getId(), pageable);
        }

        boolean hasNext = users.size() > request.getSize();
        if (hasNext) {
            users = users.subList(0, request.getSize());
        }

        List<OtherProfileDTO> profileDTOs = new ArrayList<>();
        for (User user : users) {
            profileDTOs.add(UserMapper.mapUserToOtherUserDTO(user));
        }

        String nextCursor = null;
        if (!users.isEmpty()) {
            PageCursor<Long> cursorData = new PageCursor<>(pageNumber+1,null, users.get(users.size() - 1).getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<OtherProfileDTO>> response =
                new CursorPaginationResponse<>(profileDTOs, nextCursor, hasNext);

        logger.debug("Found {} user(s)", profileDTOs.size());
        return response;
    }

    public CursorPaginationResponse<List<OtherProfileDTO>> findUserByUsernameContaining(
            String keyword, CursorPaginationRequest request) {

        logger.debug("Fetching users by username containing keyword, keyword={}", keyword);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<User> users;
        long pageNumber=0;
        if (request.getCursor() == null) {
            users = userRepository.findByUsernameContainingOfFirstPage(keyword, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            pageNumber=cursorData.getPageNumber();
            users = userRepository.findByUsernameContainingOfNextPage(keyword, cursorData.getId(), pageable);
        }

        boolean hasNext = users.size() > request.getSize();
        if (hasNext) {
            users = users.subList(0, request.getSize());
        }

        List<OtherProfileDTO> profileDTOs = new ArrayList<>();
        for (User user : users) {
            profileDTOs.add(UserMapper.mapUserToOtherUserDTO(user));
        }

        String nextCursor = null;
        if (!users.isEmpty()) {
            PageCursor<Long> cursorData = new PageCursor<>(pageNumber+1,null, users.get(users.size() - 1).getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<OtherProfileDTO>> response =
                new CursorPaginationResponse<>(profileDTOs, nextCursor, hasNext);

        logger.debug("Found {} user(s) matching keyword={}", profileDTOs.size(), keyword);
        return response;
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "admin_Users", allEntries = true),
                    @CacheEvict(value = "users_Id", key = "#id")
            }
    )
    public void changeActiveStatusForUser(Long id, boolean newStatus) {

        User user;
        try {
            user = userRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new UserNotFoundException(id));
        } catch (PessimisticLockException | jakarta.persistence.LockTimeoutException e) {
            throw new LockTimeoutException(EntityType.USER, id);
        }

        if (user.isActive() == newStatus) {
            throw new InvalidRequestException("User already has the requested active status, userId= "+id+", status= "+newStatus);
        }

        user.setActive(newStatus);
        userRepository.save(user);
        messagingTemplate.convertAndSend("/topic/admin",
                new ChatEvent<>(EventType.USER_ACTIVE_STATUS_CHANGED, UserMapper.mapUserToOtherUserDTO(user)));
    }

    //CHATBOX

    @Cacheable(
            value = "admin_Chatboxes",
            key = "@cursorHelper.extractPageNumber(#request.cursor)",
            condition = "@cursorHelper.extractPageNumber(#request.cursor) < 5"
    )
    public CursorPaginationResponse<List<ChatBoxDTO>> getAllChatBox(CursorPaginationRequest request) {
        logger.debug("Fetching all chatboxes");

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        long pageNumber=0;
        if (request.getCursor() == null) {
            chatBoxes = chatBoxRepository.findAllOfFirstPage(pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            pageNumber=cursorData.getPageNumber();
            chatBoxes = chatBoxRepository.findAllOfNextPage(cursorData.getId(), pageable);
        }

        boolean hasNext = chatBoxes.size() > request.getSize();
        if (hasNext) {
            chatBoxes = chatBoxes.subList(0, request.getSize());
        }

        Set<Long> chatBoxIds = chatBoxes.stream()
                .map(ChatBox::getId)
                .collect(Collectors.toSet());

        List<ChatBox> confirmedChatBoxes = chatBoxIds.isEmpty()
                ? List.of()
                : chatBoxRepository.findByIdInWithUsers(new ArrayList<>(chatBoxIds));

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        String nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            PageCursor<Long> cursorData = new PageCursor<>(pageNumber+1,null, chatBoxes.get(chatBoxes.size() - 1).getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<ChatBoxDTO>> response =
                new CursorPaginationResponse<>(chatBoxDTOs, nextCursor, hasNext);

        logger.debug("Found {} chatbox(es)", chatBoxDTOs.size());
        return response;
    }

    public CursorPaginationResponse<List<ChatBoxDTO>> getAllUserChatBox(
            Long userId, CursorPaginationRequest request) {

        logger.debug("Fetching all chatboxes for userId={}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        long pageNumber=0;
        if (request.getCursor() == null) {
            chatBoxes = chatBoxRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            pageNumber=cursorData.getPageNumber();
            chatBoxes = chatBoxRepository.findByUserIdOfNextPage(
                    userId, cursorData.getTimestamp(), cursorData.getId(), pageable);
        }

        boolean hasNext = chatBoxes.size() > request.getSize();
        if (hasNext) {
            chatBoxes = chatBoxes.subList(0, request.getSize());
        }

        Set<Long> chatBoxIds = chatBoxes.stream()
                .map(ChatBox::getId)
                .collect(Collectors.toSet());

        List<ChatBox> confirmedChatBoxes = chatBoxIds.isEmpty()
                ? List.of()
                : chatBoxRepository.findByIdInWithUsers(new ArrayList<>(chatBoxIds));

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        String nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            ChatBox lastChatBox = chatBoxes.get(chatBoxes.size() - 1);
            PageCursor<Long> cursorData = new PageCursor<>(pageNumber+1,lastChatBox.getLastActiveTime(), lastChatBox.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<ChatBoxDTO>> response =
                new CursorPaginationResponse<>(chatBoxDTOs, nextCursor, hasNext);

        logger.debug("Found {} chatbox(es) for userId={}", chatBoxDTOs.size(), userId);
        return response;
    }

    //STATISTICS

    @Cacheable(value = "stats_userCount")
    public Long countUsers(){
        logger.debug("Counting total users");
        Long count = userRepository.count();
        logger.debug("Total users count={}", count);
        return count;
    }

    @Cacheable(value = "stats_userCountByActive", key = "#isActive")
    public Long countUsersByActiveStatus(Boolean isActive){
        logger.debug("Counting users by active status, isActive={}", isActive);
        Long count = userRepository.countByIsActive(isActive);
        logger.debug("Users count for isActive={} is {}", isActive, count);
        return count;
    }

    @Cacheable(value = "stats_chatBoxCount")
    public Long countChatBoxes(){
        logger.debug("Counting total chatboxes");
        Long count = chatBoxRepository.count();
        logger.debug("Total chatboxes count={}", count);
        return count;
    }

    @Cacheable(value = "stats_chatBoxCountByRange", key = "#startTime + '_' + #endTime")
    public Long countChatBoxesByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting chatboxes with lastActiveTime between startTime={} and endTime={}", startTime, endTime);
        Long count = chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
        logger.debug("Chatboxes count in given time range={}", count);
        return count;
    }

    @Cacheable(value = "stats_messageCount")
    public Long countMessages(){
        logger.debug("Counting total messages");
        Long count = messageRepository.count();
        logger.debug("Total messages count={}", count);
        return count;
    }

    @Cacheable(value = "stats_messageCountByRange", key = "#startTime + '_' + #endTime")
    public Long countMessagesByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting messages with timestamp between startTime={} and endTime={}", startTime, endTime);
        Long count = messageRepository.countByTimestampBetween(startTime,endTime);
        logger.debug("Messages count in given time range={}", count);
        return count;
    }
}