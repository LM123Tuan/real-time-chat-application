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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GroupChatService {
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupChatService(GroupChatRepository groupChatRepository, UserRepository userRepository) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

    public boolean createGroupChat(User creator, List<User> otherUsers){
        if(otherUsers==null||otherUsers.size()<=1){
            return false;
        }else{
            List<User> users=new ArrayList<>();
            users.add(creator);
            users.addAll(otherUsers);
            StringBuilder sb=new StringBuilder();
            for(User user:users){
                sb.append(", "+user.getUsername());
            }
            sb.delete(0,1);
            String name=sb.toString();
            GroupChat groupChat=new GroupChat(LocalDateTime.now(), users, creator, name, LocalDateTime.now());
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

    public boolean addMemberToGroup(Long groupChatId, Long memberId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> user=userRepository.findById(memberId);
        if(groupChat.isPresent() && user.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualUser=user.get();
            List<User> users=actualGroupChat.getUsers();
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
    }

    public boolean removeUserFromGroup(Long requesterId, Long groupChatId, Long memberId){
        Optional<GroupChat> groupChat=groupChatRepository.findById(groupChatId);
        Optional<User> member=userRepository.findById(memberId);
        Optional<User> requester=userRepository.findById(requesterId);
        if(groupChat.isPresent() && member.isPresent() && requester.isPresent()){
            GroupChat actualGroupChat=groupChat.get();
            User actualMember=member.get();
            User requesterUser=requester.get();
            User creator=actualGroupChat.getCreator();
            List<User> users=actualGroupChat.getUsers();
            if(actualMember.getId().equals(creator.getId())){
                return false;
            }else if(!users.contains(actualMember)){
                return false;
            }else if(!requesterUser.getId().equals(creator.getId())){
                return false;
            }else{
                users.remove(actualMember);
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
            User creator=actualGroupChat.getCreator();
            List<User> users=actualGroupChat.getUsers();
            if(requesterUser.getId().equals(creator.getId())){
                return false;
            }else if(!users.contains(requesterUser)){
                return false;
            }else{
                users.remove(requesterUser);
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
}
