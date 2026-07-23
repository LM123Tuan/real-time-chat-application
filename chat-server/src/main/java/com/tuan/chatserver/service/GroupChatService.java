package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.GroupChatMapper;
import com.tuan.chatserver.repository.GroupChatRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link GroupChat} (nhóm chat).
 * <p>
 * Cung cấp chức năng tạo, đổi tên, quản lý thành viên (thêm/xóa/rời nhóm), phân quyền
 * leader và vice leader, cùng các chức năng truy vấn nhóm chat theo người dùng hoặc từ khóa.
 * <p>
 * <b>Quy tắc phân quyền:</b> một nhóm chat có thể có nhiều leader và vice leader.
 * Các thao tác thăng cấp/hạ cấp và loại bỏ thành viên đều yêu cầu requester phải có
 * vai trò phù hợp (leader hoặc vice leader) và không được tự áp dụng thao tác lên chính mình.
 * <p>
 * <b>Lưu ý về log:</b> các operation ghi/sửa dữ liệu được ghi log ở mức INFO/WARN/ERROR;
 * các operation chỉ đọc dữ liệu (query) được ghi log ở mức DEBUG.
 */
@Service
public class GroupChatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    /**
     * Khởi tạo {@code GroupChatService} thông qua Constructor Injection.
     *
     * @param groupChatRepository repository dùng để thao tác dữ liệu {@link GroupChat}
     * @param userRepository      repository dùng để thao tác dữ liệu {@link User}
     */
    @Autowired
    public GroupChatService(GroupChatRepository groupChatRepository, UserRepository userRepository) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy entity {@link GroupChat} theo id, ném exception nếu không tìm thấy.
     *
     * @param groupChatId id của nhóm chat cần lấy
     * @return entity {@link GroupChat} tương ứng
     * @throws ChatBoxNotFoundException nếu không tìm thấy nhóm chat với id tương ứng
     */
    private GroupChat mapOptionalGroupChatToEntity(Long groupChatId){
        GroupChat groupChat=groupChatRepository.findById(groupChatId).orElseThrow(() -> {
            logger.warn("Add member to group chat failed: group chat not found, groupChatId={}", groupChatId);
            throw new ChatBoxNotFoundException(groupChatId);
        });
        return groupChat;
    }

    /**
     * Lấy entity {@link User} theo id, ném exception nếu không tìm thấy.
     *
     * @param userId id của người dùng cần lấy
     * @return entity {@link User} tương ứng
     * @throws UserNotFoundException nếu không tìm thấy người dùng với id tương ứng
     */
    private User mapOptionalUserToEntity(Long userId){
        User user=userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("Add member to group chat failed: member not found, memberId={}", userId);
            throw new UserNotFoundException(userId);
        });
        return user;
    }

    /**
     * Tạo một nhóm chat mới (group chat) với người tạo và ít nhất 2 thành viên khác.
     * <p>
     * Người tạo ({@code creatorId}) sẽ tự động được thêm vào danh sách thành viên và
     * được gán làm leader duy nhất của nhóm. Tên nhóm được sinh tự động bằng cách nối
     * username của tất cả thành viên. {@code otherUserIds} phải chứa ít nhất 2 người dùng
     * khác ngoài người tạo.
     *
     * @param creatorId    id của người dùng tạo nhóm chat (sẽ trở thành leader)
     * @param otherUserIds tập id của các thành viên khác cần thêm vào nhóm (phải có ít nhất 2 phần tử)
     * @throws NotEnoughMembersException nếu {@code otherUserIds} là null hoặc có ít hơn 2 phần tử
     * @throws UserNotFoundException     nếu không tìm thấy creator hoặc một trong các thành viên khác
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void createGroupChat(Long creatorId, Set<Long> otherUserIds){
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
            }catch (Exception e){
                logger.error("Error occurred while creating group chat", e);
                throw new DataAccessFailureException(e);
            }
        }
    }

    /**
     * Đổi tên một nhóm chat.
     *
     * @param groupChatId id của nhóm chat cần đổi tên
     * @param newName     tên mới của nhóm chat
     * @throws ChatBoxNotFoundException   nếu không tìm thấy nhóm chat với id tương ứng
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void renameGroupChat(Long groupChatId, String newName){
        logger.info("Attempting to rename group chat, groupChatId={}", groupChatId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        groupChat.setName(newName);
        try{
            groupChatRepository.save(groupChat);
            logger.info("Group chat renamed successfully, groupChatId={}", groupChatId);
        }catch (Exception e){
            logger.error("Error occurred while renaming group chat, groupChatId={}", groupChatId, e);
            throw new DataAccessFailureException(e);
        }
    }

    /**
     * Thêm một thành viên mới vào nhóm chat.
     * <p>
     * Chỉ những người dùng đang là thành viên của nhóm mới có quyền thêm người mới.
     * Thành viên được thêm không được đã có mặt trong nhóm.
     *
     * @param requesterId id của người dùng thực hiện thao tác thêm (phải đang là thành viên nhóm)
     * @param groupChatId id của nhóm chat cần thêm thành viên
     * @param memberId    id của người dùng được thêm vào nhóm
     * @throws ChatBoxNotFoundException      nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException         nếu không tìm thấy requester hoặc member
     * @throws UserNotInChatBoxException     nếu requester không phải là thành viên của nhóm
     * @throws UserAlreadyInChatBoxException nếu member đã là thành viên của nhóm
     * @throws DataAccessFailureException    nếu lỗi khi lưu vào database
     */
    public void addMemberToGroup(Long requesterId, Long groupChatId, Long memberId){
        logger.info("Attempting to add member to group, requesterId={}, groupChatId={}, memberId={}", requesterId, groupChatId, memberId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        User requester=mapOptionalUserToEntity(requesterId);
        User user=mapOptionalUserToEntity(memberId);
        Set<User> users=groupChat.getUsers();
        if(users.contains(requester)){
            if(!users.contains(user)){
                users.add(user);
                try{
                    groupChatRepository.save(groupChat);
                    logger.info("Member added to group successfully, groupChatId={}, memberId={}", groupChatId, memberId);
                }catch (Exception e){
                    logger.error("Error occurred while adding member to group, groupChatId={}, memberId={}", groupChatId, memberId, e);
                    throw new DataAccessFailureException(e);
                }
            }else{
                logger.warn("Add member failed: member already in group, groupChatId={}, memberId={}", groupChatId, memberId);
                throw new UserAlreadyInChatBoxException(groupChatId, memberId);
            }
        }else{
            logger.warn("Add member failed: requester not in group, groupChatId={}, requesterId={}", groupChatId, requesterId);
            throw new UserNotInChatBoxException(groupChatId, requesterId);
        }
    }

    /**
     * Loại bỏ một thành viên khỏi nhóm chat.
     * <p>
     * Requester không được tự loại bỏ chính mình (dùng {@link #outGroupChat} để tự rời nhóm).
     * Cả requester và member đều phải là thành viên của nhóm. Requester phải là leader hoặc
     * vice leader mới có quyền loại bỏ người khác. Vice leader không được loại bỏ một
     * vice leader khác, và không ai được phép loại bỏ một leader.
     *
     * @param requesterId id của người dùng thực hiện loại bỏ (phải là leader hoặc vice leader)
     * @param groupChatId id của nhóm chat
     * @param memberId    id của thành viên bị loại bỏ khỏi nhóm
     * @throws ChatBoxNotFoundException        nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException           nếu không tìm thấy requester hoặc member
     * @throws InvalidChatBoxOperationException nếu requester tự loại bỏ chính mình,
     *         requester không phải leader/vice leader, vice leader cố loại bỏ một vice leader khác,
     *         hoặc cố loại bỏ một leader
     * @throws UserNotInChatBoxException       nếu requester hoặc member không thuộc nhóm chat
     * @throws DataAccessFailureException      nếu lỗi khi lưu vào database
     */
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

    /**
     * Cho phép một người dùng tự rời khỏi nhóm chat.
     * <p>
     * Nếu người rời đi là leader duy nhất của nhóm, quyền leader sẽ được tự động chuyển giao
     * theo thứ tự ưu tiên: chuyển cho một vice leader (nếu có), nếu không thì chuyển cho
     * một thành viên bất kỳ còn lại trong nhóm; nếu nhóm không còn ai, nhóm sẽ bị đánh dấu
     * là không còn hoạt động ({@code isActive = false}).
     * Nếu người rời đi là vice leader, chỉ đơn giản bị loại khỏi danh sách vice leader.
     *
     * @param requesterId id của người dùng muốn rời khỏi nhóm
     * @param groupChatId id của nhóm chat cần rời
     * @throws ChatBoxNotFoundException   nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException      nếu không tìm thấy requester
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
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

    /**
     * Lấy thông tin nhóm chat theo id.
     *
     * @param groupChatId id của nhóm chat cần truy vấn
     * @return {@link GroupChatDTO} chứa thông tin nhóm chat
     * @throws ChatBoxNotFoundException nếu không tìm thấy nhóm chat với id tương ứng
     */
    public GroupChatDTO getGroupChatById(Long groupChatId){
        logger.debug("Fetching group chat by id, groupChatId={}", groupChatId);
        GroupChat groupChat=mapOptionalGroupChatToEntity(groupChatId);
        return GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
    }

    /**
     * Tìm kiếm các nhóm chat đang hoạt động của một người dùng theo từ khóa trong tên nhóm.
     * <p>
     * Kết quả được sắp xếp giảm dần theo thời gian hoạt động gần nhất (last active time).
     *
     * @param groupChatKeyword từ khóa cần tìm trong tên nhóm chat
     * @param userId           id của người dùng cần lọc theo (chỉ lấy nhóm mà người dùng này tham gia)
     * @return danh sách {@link GroupChatDTO} phù hợp với từ khóa và thuộc về người dùng;
     *         trả về danh sách rỗng nếu không có kết quả nào phù hợp
     */
    public List<GroupChatDTO> getGroupChatByNameContaining(String groupChatKeyword, Long userId){
        logger.debug("Fetching group chats by name containing keyword, keyword={}, userId={}", groupChatKeyword, userId);
        List<GroupChat> groupChats=groupChatRepository.findByNameContainingAndUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(groupChatKeyword,userId);
        List<GroupChatDTO> groupChatDTOs=new ArrayList<>();
        for(GroupChat groupChat:groupChats){
            GroupChatDTO groupChatDTO=GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
            groupChatDTOs.add(groupChatDTO);
        }
        logger.debug("Found {} group chats matching keyword for userId={}", groupChatDTOs.size(), userId);
        return groupChatDTOs;
    }

    /**
     * Thăng cấp một thành viên thường lên vice leader.
     * <p>
     * Chỉ leader mới có quyền thực hiện thao tác này. Requester không được tự thăng cấp
     * cho chính mình. Nominee phải là thành viên của nhóm và chưa phải là leader hoặc
     * vice leader.
     *
     * @param requesterId id của người dùng thực hiện thăng cấp (phải là leader)
     * @param groupChatId id của nhóm chat
     * @param nomineeId   id của thành viên được thăng cấp lên vice leader
     * @throws ChatBoxNotFoundException        nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException           nếu không tìm thấy requester hoặc nominee
     * @throws InvalidChatBoxOperationException nếu requester tự thăng cấp cho chính mình,
     *         requester không phải leader, hoặc nominee đã là leader/vice leader
     * @throws UserNotInChatBoxException       nếu nominee không thuộc nhóm chat
     * @throws DataAccessFailureException      nếu lỗi khi lưu vào database
     */
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

    /**
     * Thăng cấp một thành viên (thường hoặc vice leader) lên leader.
     * <p>
     * Chỉ leader mới có quyền thực hiện thao tác này. Requester không được tự thăng cấp
     * cho chính mình. Nếu nominee đang là vice leader, nominee sẽ được thêm vào danh sách
     * leader đồng thời bị loại khỏi danh sách vice leader. Nếu nominee đã là leader,
     * thao tác thất bại.
     *
     * @param requesterId id của người dùng thực hiện thăng cấp (phải là leader)
     * @param groupChatId id của nhóm chat
     * @param nomineeId   id của thành viên được thăng cấp lên leader
     * @throws ChatBoxNotFoundException        nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException           nếu không tìm thấy requester hoặc nominee
     * @throws InvalidChatBoxOperationException nếu requester tự thăng cấp cho chính mình,
     *         requester không phải leader, hoặc nominee đã là leader
     * @throws UserNotInChatBoxException       nếu nominee không thuộc nhóm chat
     * @throws DataAccessFailureException      nếu lỗi khi lưu vào database
     */
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

    /**
     * Hạ cấp một leader xuống vice leader.
     * <p>
     * Chỉ leader mới có quyền thực hiện thao tác này. Requester không được tự hạ cấp
     * cho chính mình. Nominee phải đang là leader của nhóm.
     *
     * @param requesterId id của người dùng thực hiện hạ cấp (phải là leader)
     * @param groupChatId id của nhóm chat
     * @param nomineeId   id của leader bị hạ cấp xuống vice leader
     * @throws ChatBoxNotFoundException        nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException           nếu không tìm thấy requester hoặc nominee
     * @throws InvalidChatBoxOperationException nếu requester tự hạ cấp cho chính mình,
     *         requester không phải leader, hoặc nominee không phải leader
     * @throws UserNotInChatBoxException       nếu nominee không thuộc nhóm chat
     * @throws DataAccessFailureException      nếu lỗi khi lưu vào database
     */
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

    /**
     * Hạ cấp một leader hoặc vice leader xuống thành viên thường.
     * <p>
     * Chỉ leader mới có quyền thực hiện thao tác này. Requester không được tự hạ cấp
     * cho chính mình. Nominee phải đang là leader hoặc vice leader; nếu là leader thì
     * bị loại khỏi danh sách leader, nếu là vice leader thì bị loại khỏi danh sách vice leader.
     *
     * @param requesterId id của người dùng thực hiện hạ cấp (phải là leader)
     * @param groupChatId id của nhóm chat
     * @param nomineeId   id của thành viên (leader hoặc vice leader) bị hạ xuống thành viên thường
     * @throws ChatBoxNotFoundException        nếu không tìm thấy nhóm chat với id tương ứng
     * @throws UserNotFoundException           nếu không tìm thấy requester hoặc nominee
     * @throws InvalidChatBoxOperationException nếu requester tự hạ cấp cho chính mình,
     *         requester không phải leader, hoặc nominee không phải leader/vice leader
     * @throws UserNotInChatBoxException       nếu nominee không thuộc nhóm chat
     * @throws DataAccessFailureException      nếu lỗi khi lưu vào database
     */
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

    /**
     * Lấy danh sách tất cả các nhóm chat đang hoạt động (active) mà một người dùng tham gia.
     *
     * @param userId id của người dùng cần truy vấn danh sách nhóm chat
     * @return danh sách {@link GroupChatDTO} tương ứng với các nhóm chat đang hoạt động
     *         mà người dùng tham gia; trả về danh sách rỗng nếu không có nhóm nào
     */
    public List<GroupChatDTO> getAllGroupChatByUserId(Long userId){
        logger.debug("Fetching all active group chats for userId={}", userId);
        List<GroupChat> groupChats=groupChatRepository.findByUserIdAndIsActiveTrue(userId);
        List<GroupChatDTO> groupChatDTOS=new ArrayList<>();
        for(GroupChat groupChat:groupChats){
            GroupChatDTO groupChatDTO=GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
            groupChatDTOS.add(groupChatDTO);
        }
        logger.debug("Found {} active group chats for userId={}", groupChatDTOS.size(), userId);
        return groupChatDTOS;
    }
}