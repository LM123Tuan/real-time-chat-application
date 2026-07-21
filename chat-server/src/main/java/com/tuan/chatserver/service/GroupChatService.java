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

@Service
public class GroupChatService {
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupChatService(GroupChatRepository groupChatRepository, UserRepository userRepository) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

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

    //TODO
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

    public List<GroupChatDTO> getGroupChatByNameContaining(String groupChatKeyword, Long userId){
        List<GroupChat> groupChats=groupChatRepository.findByNameContainingAndUsers_IdOrderByLastActiveTimeDesc(groupChatKeyword,userId);
        List<GroupChatDTO> groupChatDTOs=new ArrayList<>();
        for(GroupChat groupChat:groupChats){
            GroupChatDTO groupChatDTO=GroupChatMapper.mapGroupChatToGroupChatDTO(groupChat);
            groupChatDTOs.add(groupChatDTO);
        }
        return groupChatDTOs;
    }

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
}
