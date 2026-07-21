package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.GroupChatDTO;
import com.tuan.chatserver.entity.GroupChat;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.mapper.GroupChatMapper;
import com.tuan.chatserver.repository.GroupChatRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link GroupChat} (đoạn chat nhóm nhiều người dùng).
 * <p>
 * Bao gồm các chức năng: tạo nhóm chat, đổi tên nhóm, thêm/xóa thành viên, rời nhóm,
 * truy vấn nhóm chat theo id hoặc theo tên, cùng các thao tác phân quyền
 * (thăng cấp/hạ cấp giữa leader, vice leader và thành viên thường).
 */
@Service
public class GroupChatService {
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
     * Tạo một nhóm chat mới với người tạo là leader mặc định.
     * <p>
     * Nhóm chat hợp lệ phải có ít nhất 2 thành viên khác ngoài người tạo
     * (tổng cộng ít nhất 3 người). Tên nhóm được tự động sinh ra bằng cách nối username
     * của tất cả thành viên, cách nhau bởi dấu phẩy.
     *
     * @param creator    người dùng tạo nhóm, sẽ tự động trở thành leader
     * @param otherUsers tập hợp các thành viên khác được thêm vào nhóm (không tính creator)
     * @return {@code true} nếu tạo và lưu nhóm chat thành công; {@code false} nếu
     *         {@code otherUsers} là {@code null}, có ít hơn 2 thành viên, hoặc xảy ra lỗi khi lưu
     */
    public boolean createGroupChat(User creator, Set<User> otherUsers){
        if(otherUsers==null||otherUsers.size()<=1){
            return false;
        }else{
            Set<User> users=new HashSet<>();
            users.add(creator);
            users.addAll(otherUsers);
            Set<User> leaders=new HashSet<>();
            leaders.add(creator);
            Set<User> viceLeaders=new HashSet<>();
            StringBuilder sb=new StringBuilder();
            for(User user:users){
                sb.append(", "+user.getUsername());
            }
            sb.delete(0,1);
            String name=sb.toString();
            GroupChat groupChat=new GroupChat(LocalDateTime.now(), users, leaders, viceLeaders, name, true, LocalDateTime.now());
            try{
                groupChatRepository.save(groupChat);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Đổi tên của một nhóm chat đã tồn tại.
     *
     * @param groupChatId id của nhóm chat cần đổi tên
     * @param newName     tên mới cho nhóm chat
     * @return {@code true} nếu đổi tên và lưu thành công; {@code false} nếu không tìm thấy
     *         nhóm chat với id tương ứng hoặc xảy ra lỗi khi lưu
     */
    public boolean renameGroupChat(Long groupChatId, String newName){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        if(groupChat.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            actualGroupChat.setName(newName);
            try{
                groupChatRepository.save(actualGroupChat);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    /**
     * Thêm một thành viên mới vào nhóm chat.
     * <p>
     * Yêu cầu người thực hiện (requester) phải là thành viên hiện tại của nhóm.
     * Thành viên được thêm vào không được đã có mặt trong nhóm.
     *
     * @param requesterId id của người dùng thực hiện yêu cầu thêm thành viên
     * @param groupChatId id của nhóm chat cần thêm thành viên
     * @param memberId    id của người dùng sẽ được thêm vào nhóm
     * @return {@code true} nếu thêm thành viên và lưu thành công; {@code false} nếu
     *         nhóm chat/requester/member không tồn tại, requester không thuộc nhóm,
     *         member đã có trong nhóm, hoặc xảy ra lỗi khi lưu
     */
    public boolean addMemberToGroup(Long requesterId, Long groupChatId, Long memberId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        Optional<User> user=userRepository.findById(memberId);
        if(groupChat.isPresent() && requester.isPresent() && user.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualRequester=requester.get();
            User actualUser=user.get();
            Set<User> users=actualGroupChat.getUsers();
            if(users.contains(actualRequester)){
                if(!users.contains(actualUser)){
                    users.add(actualUser);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch (Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    /**
     * Xóa một thành viên ra khỏi nhóm chat (do người khác thực hiện, ví dụ leader/vice leader).
     * <p>
     * Các điều kiện để thao tác hợp lệ:
     * <ul>
     *     <li>Requester và member phải khác nhau.</li>
     *     <li>Cả requester và member đều phải là thành viên của nhóm.</li>
     *     <li>Requester phải là leader hoặc vice leader.</li>
     *     <li>Vice leader không được xóa một vice leader khác.</li>
     *     <li>Không ai được xóa một leader.</li>
     * </ul>
     * Nếu member bị xóa đang là vice leader thì đồng thời bị loại khỏi danh sách vice leader.
     *
     * @param requesterId id của người dùng thực hiện việc xóa thành viên
     * @param groupChatId id của nhóm chat
     * @param memberId    id của thành viên bị xóa khỏi nhóm
     * @return {@code true} nếu xóa và lưu thành công; {@code false} nếu vi phạm một trong
     *         các điều kiện trên, dữ liệu không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean removeUserFromGroup(Long requesterId, Long groupChatId, Long memberId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> member=userRepository.findById(memberId);
        Optional<User> requester=userRepository.findById(requesterId);
        if(groupChat.isPresent() && member.isPresent() && requester.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualMember=member.get();
            User requesterUser=requester.get();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            Set<User> users=actualGroupChat.getUsers();
            if(requesterUser.equals(actualMember)){
                return false;
            }else if(!users.contains(requesterUser) || !users.contains(actualMember)){
                return false;
            }else if(!leaders.contains(requesterUser) && !viceLeaders.contains(requesterUser)){
                return false;
            }else if(viceLeaders.contains(actualMember) && viceLeaders.contains(requesterUser)){
                return false;
            }else if(leaders.contains(actualMember)){
                return false;
            }else{
                users.remove(actualMember);
                if(viceLeaders.contains(actualMember)){
                    viceLeaders.remove(actualMember);
                }
                try{
                    groupChatRepository.save(actualGroupChat);
                    return true;
                }catch (Exception e){
                    e.printStackTrace();
                    return false;
                }
            }
        }else{
            return false;
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
     * @return {@code true} nếu rời nhóm và lưu thành công; {@code false} nếu nhóm chat
     *         hoặc requester không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean outGroupChat(Long requesterId, Long groupChatId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        if(groupChat.isPresent() && requester.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User requesterUser=requester.get();
            Set<User> users=actualGroupChat.getUsers();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            users.remove(requesterUser);
            int leadersSize=leaders.size();
            int viceLeadersSize=viceLeaders.size();
            int usersSize=users.size();
            if(leaders.contains(requesterUser)){
                if(leadersSize==1){
                    if(viceLeadersSize>=1){
                        User nextLeader=viceLeaders.iterator().next();
                        leaders.remove(requesterUser);
                        viceLeaders.remove(nextLeader);
                        leaders.add(nextLeader);
                    }else{
                        if(usersSize>=1){
                            User nextLeader=users.iterator().next();
                            leaders.remove(requesterUser);
                            leaders.add(nextLeader);
                        }else{
                            leaders.remove(requesterUser);
                            actualGroupChat.setActive(false);
                        }
                    }
                }else{
                    leaders.remove(requesterUser);
                }
            }else if(viceLeaders.contains(requesterUser)){
                viceLeaders.remove(requesterUser);
            }
            try{
                groupChatRepository.save(actualGroupChat);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    /**
     * Lấy thông tin nhóm chat theo id.
     *
     * @param groupChatId id của nhóm chat cần truy vấn
     * @return {@link GroupChatDTO} chứa thông tin nhóm chat nếu tìm thấy;
     *         trả về {@code null} nếu không tồn tại nhóm chat với id tương ứng
     */
    public GroupChatDTO getGroupChatById(Long groupChatId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        if(groupChat.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            GroupChatDTO groupChatDTO= GroupChatMapper.mapGroupChatToGroupChatDTO(actualGroupChat);
            return groupChatDTO;
        }else{
            return null;
        }
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
        List<GroupChat> groupChats=groupChatRepository.findByNameContainingAndUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(groupChatKeyword,userId);
        List<GroupChatDTO> groupChatDTOs=new ArrayList<>();
        for(GroupChat groupChat:groupChats){
            GroupChatDTO groupChatDTO=GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
            groupChatDTOs.add(groupChatDTO);
        }
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
     * @return {@code true} nếu thăng cấp và lưu thành công; {@code false} nếu vi phạm
     *         điều kiện trên, dữ liệu không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean promoteToViceLeader(Long requesterId, Long groupChatId, Long nomineeId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        Optional<User> nominee=userRepository.findById(nomineeId);
        if(groupChat.isPresent() && requester.isPresent() && nominee.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualRequester=requester.get();
            User actualNominee=nominee.get();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            Set<User> users=actualGroupChat.getUsers();
            if(actualNominee.equals(actualRequester)){
                return false;
            }
            if(leaders.contains(actualRequester) && users.contains(actualNominee)){
                if(!viceLeaders.contains(actualNominee) && !leaders.contains(actualNominee)){
                    viceLeaders.add(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
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
     * @return {@code true} nếu thăng cấp và lưu thành công; {@code false} nếu vi phạm
     *         điều kiện trên, dữ liệu không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean promoteToLeader(Long requesterId, Long groupChatId, Long nomineeId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        Optional<User> nominee=userRepository.findById(nomineeId);
        if(groupChat.isPresent() && requester.isPresent() && nominee.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualRequester=requester.get();
            User actualNominee=nominee.get();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            Set<User> users=actualGroupChat.getUsers();
            if(actualNominee.equals(actualRequester)){
                return false;
            }
            if(leaders.contains(actualRequester) && users.contains(actualNominee)){
                if(!viceLeaders.contains(actualNominee) && !leaders.contains(actualNominee)){
                    leaders.add(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else if(!leaders.contains(actualNominee) && viceLeaders.contains(actualNominee)){
                    leaders.add(actualNominee);
                    viceLeaders.remove(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }
                else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
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
     * @return {@code true} nếu hạ cấp và lưu thành công; {@code false} nếu vi phạm
     *         điều kiện trên, dữ liệu không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean demoteToViceLeader(Long requesterId, Long groupChatId, Long nomineeId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        Optional<User> nominee=userRepository.findById(nomineeId);
        if(groupChat.isPresent() && requester.isPresent() && nominee.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualRequester=requester.get();
            User actualNominee=nominee.get();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            Set<User> users=actualGroupChat.getUsers();
            if(actualNominee.equals(actualRequester)){
                return false;
            }
            if(leaders.contains(actualRequester) && users.contains(actualNominee)){
                if(leaders.contains(actualNominee)){
                    leaders.remove(actualNominee);
                    viceLeaders.add(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
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
     * @return {@code true} nếu hạ cấp và lưu thành công; {@code false} nếu vi phạm
     *         điều kiện trên, dữ liệu không tồn tại, hoặc xảy ra lỗi khi lưu
     */
    public boolean demoteToUser(Long requesterId, Long groupChatId, Long nomineeId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> requester=userRepository.findById(requesterId);
        Optional<User> nominee=userRepository.findById(nomineeId);
        if(groupChat.isPresent() && requester.isPresent() && nominee.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualRequester=requester.get();
            User actualNominee=nominee.get();
            Set<User> leaders=actualGroupChat.getLeaders();
            Set<User> viceLeaders=actualGroupChat.getViceLeaders();
            Set<User> users=actualGroupChat.getUsers();
            if(actualNominee.equals(actualRequester)){
                return false;
            }
            if(leaders.contains(actualRequester) && users.contains(actualNominee)){
                if(leaders.contains(actualNominee)){
                    leaders.remove(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else if(viceLeaders.contains(actualNominee)){
                    viceLeaders.remove(actualNominee);
                    try{
                        groupChatRepository.save(actualGroupChat);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }
                else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
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
        List<GroupChat> groupChats=groupChatRepository.findByUserIdAndIsActiveTrue(userId);
        List<GroupChatDTO> groupChatDTOS=new ArrayList<>();
        for(GroupChat groupChat:groupChats){
            GroupChatDTO groupChatDTO=GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
            groupChatDTOS.add(groupChatDTO);
        }
        return groupChatDTOS;
    }
}