package com.tuan.chatserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.GroupChatPermission;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.GroupChatMapper;
import com.tuan.chatserver.repository.GroupChatRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.util.CursorCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupChatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final CursorCodec cursorCodec;

    @Autowired
    public GroupChatService(GroupChatRepository groupChatRepository,
                            UserRepository userRepository,
                            CursorCodec cursorCodec) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
        this.cursorCodec=cursorCodec;
    }

    private GroupChat mapOptionalGroupChatToEntity(Long groupChatId){
        GroupChat groupChat=groupChatRepository.findById(groupChatId).orElseThrow(() -> {
            logger.warn("Add member to group chat failed: group chat not found, groupChatId={}", groupChatId);
            throw new ChatBoxNotFoundException(groupChatId);
        });
        return groupChat;
    }

    private User mapOptionalUserToEntity(Long userId){
        User user=userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("Add member to group chat failed: member not found, memberId={}", userId);
            throw new UserNotFoundException(userId);
        });
        return user;
    }

    private Set<User> mapOptionalUsersToUserEntities(Set<Long> userIds){
        Set<User> users = new HashSet<>();
        for(Long id:userIds){
            User user = mapOptionalUserToEntity(id);
            users.add(user);
        }
        return users;
    }

    public GroupChatDTO createGroupChat(Long creatorId, Set<Long> otherUserIds){
        logger.info("Attempting to create group chat, creatorId={}", creatorId);
        if(otherUserIds==null||otherUserIds.size()<=1){
            logger.warn("Create group chat failed: otherUsers is null or has less than 2 members");
            throw new NotEnoughMembersException();
        }else{
            Set<Long> userIds=new HashSet<>();
            userIds.addAll(otherUserIds);
            userIds.add(creatorId);
            Set<User> users=userRepository.findByIdIn(userIds);
            User leader = users.stream().filter(user -> user.getId().equals(creatorId)).findFirst().orElseThrow(() -> new UserNotFoundException(creatorId));
            if(users.size()!=userIds.size()){
                Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
                Set<Long> missingIds = new HashSet<>(otherUserIds);
                missingIds.removeAll(foundIds);
                throw new UserNotFoundException(missingIds);
            }
            Set<User> leaders=new HashSet<>();
            leaders.add(leader);
            Set<User> viceLeaders=new HashSet<>();
            String name=users.stream().map(User::getUsername).collect(Collectors.joining(", "));
            GroupChat groupChat=new GroupChat(LocalDateTime.now(), users, leaders, viceLeaders, name, true, LocalDateTime.now());
            try{
                groupChatRepository.save(groupChat);
                logger.info("Group chat created successfully, id={}, name={}", groupChat.getId(), name);
                GroupChatDTO dto = GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
                return dto;
            }catch (Exception e){
                logger.error("Error occurred while creating group chat", e);
                throw new DataAccessFailureException(e);
            }
        }
    }

    public void renameGroupChat(Long requesterId, RenameGroupChatRequest request){
        Long groupChatId = request.getGroupChatId();
        String newName = request.getNewName();
        logger.info("Attempting to rename group chat, groupChatId={}", groupChatId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester = userRepository.findById(requesterId).orElseThrow(() -> {
            throw new UserNotFoundException(requesterId);
        });
        if(!groupChat.getUsers().contains(requester)){
            throw new UserNotInChatBoxException(groupChatId, requesterId);
        }
        groupChat.setName(newName);
        try{
            groupChatRepository.save(groupChat);
            logger.info("Group chat renamed successfully, groupChatId={}", groupChatId);
        }catch (Exception e){
            logger.error("Error occurred while renaming group chat, groupChatId={}", groupChatId, e);
            throw new DataAccessFailureException(e);
        }
    }

    private Set<User> checkUserIsAlreadyInGroupChat(Set<User> users, Set<User> requesters){
        Set<User> res= new HashSet<>();
        for(User user:requesters){
            if(users.contains(user)){
                res.add(user);
            }
        }
        return res;
    }

    private Set<Long> mapUsersToUserIds(Set<User> users){
        Set<Long> res= new HashSet<>();
        for(User user:users){
            res.add(user.getId());
        }
        return res;
    }

    public void addMembersToGroup(Long requesterId, Long groupChatId, Set<Long> otherUserIds){
        logger.info("Attempting to add member to group, requesterId={}, groupChatId={}, memberIds={}", requesterId, groupChatId, otherUserIds);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        Set<User> addedUsers=mapOptionalUsersToUserEntities(otherUserIds);
        Set<User> users=groupChat.getUsers();
        if(users.contains(requester)){
            Set<User> checkUser = checkUserIsAlreadyInGroupChat(users, addedUsers);
            if(checkUser.isEmpty()){
                users.addAll(addedUsers);
                try{
                    groupChatRepository.save(groupChat);
                    logger.info("Member added to group successfully, groupChatId={}, memberIds={}", groupChatId, otherUserIds);
                }catch (Exception e){
                    logger.error("Error occurred while adding member to group, groupChatId={}, memberId={}", groupChatId, otherUserIds, e);
                    throw new DataAccessFailureException(e);
                }
            }else{
                logger.warn("Add member failed: member already in group, groupChatId={}, memberId={}", groupChatId, otherUserIds);
                Set<Long> userIds=mapUsersToUserIds(checkUser);
                throw new UserAlreadyInChatBoxException(groupChatId, userIds);
            }
        }else{
            logger.warn("Add member failed: requester not in group, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new UserNotInChatBoxException(groupChatId, requesterId);
        }
    }

    public void removeUserFromGroup(Long requesterId, Long groupChatId, Long memberId){
        logger.info("Attempting to remove member from group, requesterId={}, groupChatId={}, memberId={}", requesterId, groupChatId, memberId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User member=mapOptionalUserToEntity(memberId);
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        Set<User> users=groupChat.getUsers();
        if(requester.equals(member)){
            logger.warn("Remove member failed: requester cannot remove themselves, requesterId={}", requesterId);
            throw new InvalidChatBoxOperationException("Requester cannot remove themselves from groupId: "+groupChatId);
        }else if(!users.contains(requester) || !users.contains(member)){
            logger.warn("Remove member failed: requester or member not in group, chatBoxId={}, requesterId={}, memberId={}", groupChatId, requesterId, memberId);
            Set<Long> ids=Set.of(requesterId, memberId);
            throw new UserNotInChatBoxException(groupChatId, ids);
        }else if(!leaders.contains(requester) && !viceLeaders.contains(requester)){
            logger.warn("Remove member failed: requester is not leader or vice leader, chatBoxId={}, requesterId={}", groupChatId, requesterId);
            throw new InvalidChatBoxOperationException("user is not leader or vice leader, chatBoxId="+groupChatId+", userId="+requesterId);
        }else if(viceLeaders.contains(member) && viceLeaders.contains(member)){
            logger.warn("Remove member failed: vice leader cannot remove another vice leader, requesterId={}, memberId={}", requesterId, memberId);
            throw new InvalidChatBoxOperationException("Vice leader cannot remove another vice leader, requesterId="+requesterId+", memberId="+memberId);
        }else if(leaders.contains(member)){
            logger.warn("Remove member failed: cannot remove a leader, memberId={}", memberId);
            throw new InvalidChatBoxOperationException("Cannot remove a leader, memberId="+memberId);
        }else{
            users.remove(member);
            if(viceLeaders.contains(member)){
                viceLeaders.remove(member);
            }
            try{
                groupChatRepository.save(groupChat);
                logger.info("Member removed from group successfully, groupChatId={}, memberId={}", groupChatId, memberId);
            }catch (Exception e){
                logger.error("Error occurred while removing member from group, groupChatId={}, memberId={}", groupChatId, memberId, e);
                throw new DataAccessFailureException(e);
            }
        }
    }

    public void outGroupChat(Long requesterId, Long groupChatId){
        logger.info("Attempting to leave group chat, requesterId={}, groupChatId={}", requesterId, groupChatId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        Set<User> users=groupChat.getUsers();
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        users.remove(requester);
        int leadersSize=leaders.size();
        int viceLeadersSize=viceLeaders.size();
        int usersSize=users.size();
        if(leaders.contains(requester)){
            if(leadersSize==1){
                if(viceLeadersSize>=1){
                    User nextLeader=viceLeaders.iterator().next();
                    leaders.remove(requester);
                    viceLeaders.remove(nextLeader);
                    leaders.add(nextLeader);
                }else{
                    if(usersSize>=1){
                        User nextLeader=users.iterator().next();
                        leaders.remove(requester);
                        leaders.add(nextLeader);
                    }else{
                        leaders.remove(requester);
                        groupChat.setActive(false);
                        logger.info("Group chat has no members left, marking inactive, groupChatId={}", groupChatId);
                    }
                }
            }else{
                leaders.remove(requester);
            }
        }else if(viceLeaders.contains(requester)){
            viceLeaders.remove(requester);
        }
        try{
            groupChatRepository.save(groupChat);
            logger.info("User left group chat successfully, requesterId={}, groupChatId={}", requesterId, groupChatId);
        }catch (Exception e){
            logger.error("Error occurred while leaving group chat, requesterId={}, groupChatId={}", requesterId, groupChatId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public GroupChatDTO getGroupChatById(Long userId, Long groupChatId){
        logger.debug("Fetching group chat by id, groupChatId={}", groupChatId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException(userId);
        });
        if(!groupChat.getUsers().contains(user)){
            throw new UserNotInChatBoxException(groupChatId, userId);
        }
        return GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
    }

    public CursorPaginationResponse<List<GroupChatDTO>> getGroupChatByNameContaining(String groupChatKeyword, Long userId, CursorPaginationRequest request) {
        logger.debug("Fetching group chats by name containing keyword, keyword={}, userId={}", groupChatKeyword, userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<GroupChat> groupChats;
        if (request.getCursor() == null) {
            groupChats = groupChatRepository.findByNameContainingAndUserIdOfFirstPage(groupChatKeyword, userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(
                    request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            groupChats = groupChatRepository.findByNameContainingAndUserIdOfNextPage(
                    groupChatKeyword,
                    userId,
                    cursorData.getTimestamp(),
                    cursorData.getId(),
                    pageable
            );
        }

        boolean hasNext = groupChats.size() > request.getSize();
        if (hasNext) {
            groupChats = groupChats.subList(0, request.getSize());
        }

        Set<Long> groupChatIds = groupChats.stream()
                .map(GroupChat::getId)
                .collect(Collectors.toSet());

        List<GroupChat> confirmedGroupChats = groupChatIds.isEmpty()
                ? List.of()
                : groupChatRepository.findByIdInWithUsers(new ArrayList<>(groupChatIds));

        Map<Long, GroupChat> confirmedGroupChatMap = confirmedGroupChats.stream()
                .collect(Collectors.toMap(GroupChat::getId, gc -> gc));

        List<GroupChatDTO> groupChatDTOs = new ArrayList<>();
        for (GroupChat groupChat : groupChats) {
            GroupChat confirmed = confirmedGroupChatMap.get(groupChat.getId());
            if (confirmed != null) {
                groupChatDTOs.add(GroupChatMapper.mapGroupChatToGroupChatDTO(confirmed));
            }
        }

        String nextCursor = null;
        if (!groupChats.isEmpty()) {
            GroupChat lastGroupChat = groupChats.get(groupChats.size() - 1);
            PageCursor<Long> cursorData = new PageCursor<>(lastGroupChat.getLastActiveTime(), lastGroupChat.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<GroupChatDTO>> response =
                new CursorPaginationResponse<>(groupChatDTOs, nextCursor, hasNext);

        logger.debug("Found {} group chat(s) matching keyword for userId={}", groupChatDTOs.size(), userId);
        return response;
    }

    public void promoteToViceLeader(Long requesterId, Long groupChatId, Long nomineeId){
        logger.info("Attempting to promote member to vice leader, requesterId={}, groupChatId={}, nomineeId={}", requesterId, groupChatId, nomineeId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User nominee=mapOptionalUserToEntity(nomineeId);
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        Set<User> users=groupChat.getUsers();
        if(nominee.equals(requester)){
            logger.warn("Promote to vice leader failed: requester cannot promote themselves, requesterId={}", requesterId);
            throw new InvalidChatBoxOperationException("Requester cannot promote themselves, requesterId="+requesterId);
        }
        if(!leaders.contains(requester)){
            logger.warn("Promote to vice leader failed: requester is not leader, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new InvalidChatBoxOperationException("Requester is not leader, groupChatId="+groupChatId+", requesterId="+requesterId);
        }
        if(!users.contains(nominee)){
            logger.warn("Promote to vice leader failed: nominee not in group, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
            throw new UserNotInChatBoxException(groupChatId, nomineeId);
        }
        if(viceLeaders.contains(nominee) || leaders.contains(nominee)){
            logger.warn("Promote to vice leader failed: nominee already leader or vice leader, nomineeId={}", nomineeId);
            throw new InvalidChatBoxOperationException("Nominee already leader or vice leader, nomineeId="+nomineeId);
        }
        viceLeaders.add(nominee);
        try{
            groupChatRepository.save(groupChat);
            logger.info("Member promoted to vice leader successfully, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
        }catch(Exception e){
            logger.error("Error occurred while promoting member to vice leader, groupChatId={}, nomineeId={}", groupChatId, nomineeId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void promoteToLeader(Long requesterId, Long groupChatId, Long nomineeId){
        logger.info("Attempting to promote member to leader, requesterId={}, groupChatId={}, nomineeId={}", requesterId, groupChatId, nomineeId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User nominee=mapOptionalUserToEntity(nomineeId);
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        Set<User> users=groupChat.getUsers();
        if(nominee.equals(requester)){
            logger.warn("Promote to leader failed: requester cannot promote themselves, requesterId={}", requesterId);
            throw new InvalidChatBoxOperationException("Requester cannot promote themselves, requesterId="+requesterId);
        }
        if(!leaders.contains(requester)){
            logger.warn("Promote to leader failed: requester is not leader, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new InvalidChatBoxOperationException("Requester is not leader, groupChatId="+groupChatId+", requesterId="+requesterId);
        }
        if(!users.contains(nominee)){
            logger.warn("Promote to leader failed: nominee not in group, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
            throw new UserNotInChatBoxException(groupChatId, nomineeId);
        }
        if(leaders.contains(nominee)){
            logger.warn("Promote to leader failed: nominee already a leader, nomineeId={}", nomineeId);
            throw new InvalidChatBoxOperationException("Nominee already a leader, nomineeId="+nomineeId);
        }
        boolean wasViceLeader=viceLeaders.contains(nominee);
        leaders.add(nominee);
        if(wasViceLeader){
            viceLeaders.remove(nominee);
        }
        try{
            groupChatRepository.save(groupChat);
            logger.info("Member promoted to leader successfully, groupChatId={}, nomineeId={}, fromViceLeader={}", groupChatId, nomineeId, wasViceLeader);
        }catch(Exception e){
            logger.error("Error occurred while promoting member to leader, groupChatId={}, nomineeId={}", groupChatId, nomineeId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void demoteToViceLeader(Long requesterId, Long groupChatId, Long nomineeId){
        logger.info("Attempting to demote leader to vice leader, requesterId={}, groupChatId={}, nomineeId={}", requesterId, groupChatId, nomineeId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User nominee=mapOptionalUserToEntity(nomineeId);
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        Set<User> users=groupChat.getUsers();
        if(nominee.equals(requester)){
            logger.warn("Demote to vice leader failed: requester cannot demote themselves, requesterId={}", requesterId);
            throw new InvalidChatBoxOperationException("Requester cannot demote themselves, requesterId="+requesterId);
        }
        if(!leaders.contains(requester)){
            logger.warn("Demote to vice leader failed: requester is not leader, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new InvalidChatBoxOperationException("Requester is not leader, groupChatId="+groupChatId+", requesterId="+requesterId);
        }
        if(!users.contains(nominee)){
            logger.warn("Demote to vice leader failed: nominee not in group, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
            throw new UserNotInChatBoxException(groupChatId, nomineeId);
        }
        if(!leaders.contains(nominee)){
            logger.warn("Demote to vice leader failed: nominee is not a leader, nomineeId={}", nomineeId);
            throw new InvalidChatBoxOperationException("Nominee is not a leader, nomineeId="+nomineeId);
        }
        leaders.remove(nominee);
        viceLeaders.add(nominee);
        try{
            groupChatRepository.save(groupChat);
            logger.info("Leader demoted to vice leader successfully, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
        }catch(Exception e){
            logger.error("Error occurred while demoting leader to vice leader, groupChatId={}, nomineeId={}", groupChatId, nomineeId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void demoteToUser(Long requesterId, Long groupChatId, Long nomineeId){
        logger.info("Attempting to demote member to regular user, requesterId={}, groupChatId={}, nomineeId={}", requesterId, groupChatId, nomineeId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User nominee=mapOptionalUserToEntity(nomineeId);
        Set<User> leaders=groupChat.getLeaders();
        Set<User> viceLeaders=groupChat.getViceLeaders();
        Set<User> users=groupChat.getUsers();
        if(nominee.equals(requester)){
            logger.warn("Demote to user failed: requester cannot demote themselves, requesterId={}", requesterId);
            throw new InvalidChatBoxOperationException("Requester cannot demote themselves, requesterId="+requesterId);
        }
        if(!leaders.contains(requester)){
            logger.warn("Demote to user failed: requester is not leader, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new InvalidChatBoxOperationException("Requester is not leader, groupChatId="+groupChatId+", requesterId="+requesterId);
        }
        if(!users.contains(nominee)){
            logger.warn("Demote to user failed: nominee not in group, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
            throw new UserNotInChatBoxException(groupChatId, nomineeId);
        }
        if(leaders.contains(nominee)){
            leaders.remove(nominee);
        }else if(viceLeaders.contains(nominee)){
            viceLeaders.remove(nominee);
        }else{
            logger.warn("Demote to user failed: nominee is neither leader nor vice leader, nomineeId={}", nomineeId);
            throw new InvalidChatBoxOperationException("Nominee is neither leader nor vice leader, nomineeId="+nomineeId);
        }
        try{
            groupChatRepository.save(groupChat);
            logger.info("Member demoted to regular user successfully, groupChatId={}, nomineeId={}", groupChatId, nomineeId);
        }catch(Exception e){
            logger.error("Error occurred while demoting member to user, groupChatId={}, nomineeId={}", groupChatId, nomineeId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public CursorPaginationResponse<List<GroupChatDTO>> getAllGroupChatByUserId(Long userId, CursorPaginationRequest request) {
        logger.debug("Fetching all active group chats for userId={}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<GroupChat> groupChats;
        if (request.getCursor() == null) {
            groupChats = groupChatRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(
                    request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            groupChats = groupChatRepository.findByUserIdOfNextPage(
                    userId,
                    cursorData.getTimestamp(),
                    cursorData.getId(),
                    pageable
            );
        }

        boolean hasNext = groupChats.size() > request.getSize();
        if (hasNext) {
            groupChats = groupChats.subList(0, request.getSize());
        }

        Set<Long> groupChatIds = groupChats.stream()
                .map(GroupChat::getId)
                .collect(Collectors.toSet());

        List<GroupChat> confirmedGroupChats = groupChatIds.isEmpty()
                ? List.of()
                : groupChatRepository.findByIdInWithUsers(new ArrayList<>(groupChatIds));

        Map<Long, GroupChat> confirmedGroupChatMap = confirmedGroupChats.stream()
                .collect(Collectors.toMap(GroupChat::getId, gc -> gc));

        List<GroupChatDTO> groupChatDTOs = new ArrayList<>();
        for (GroupChat groupChat : groupChats) {
            GroupChat confirmed = confirmedGroupChatMap.get(groupChat.getId());
            if (confirmed != null) {
                groupChatDTOs.add(GroupChatMapper.mapGroupChatToGroupChatDTO(confirmed));
            }
        }

        String nextCursor = null;
        if (!groupChats.isEmpty()) {
            GroupChat lastGroupChat = groupChats.get(groupChats.size() - 1);
            PageCursor<Long> cursorData = new PageCursor<>(lastGroupChat.getLastActiveTime(), lastGroupChat.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<GroupChatDTO>> response =
                new CursorPaginationResponse<>(groupChatDTOs, nextCursor, hasNext);

        logger.debug("Found {} active group chat(s) for userId={}", groupChatDTOs.size(), userId);
        return response;
    }

    public GroupChatPermission getGroupChatPermission(Long userId, Long groupChatId){
        GroupChat groupChat = groupChatRepository.findById(groupChatId).orElseThrow(() -> {
            throw new ChatBoxNotFoundException(groupChatId);
        });
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException(userId);
        });
        if(groupChat.getLeaders().contains(user)){
            return GroupChatPermission.LEADER;
        }else if(groupChat.getViceLeaders().contains(user)){
            return GroupChatPermission.VICE_LEADER;
        }else{
            return GroupChatPermission.USER;
        }
    }
}