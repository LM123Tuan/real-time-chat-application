package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository, UserRepository userRepository) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
    }

    public boolean createDirectMessage(Long creatorId, Long receiverId) {
        Optional<User> creator = userRepository.findById(creatorId);
        Optional<User> receiver = userRepository.findById(receiverId);
        if(creator.isPresent() && receiver.isPresent()){
            if(!directMessageRepository.existsBetweenTwoUsers(creatorId,receiverId)){
                User actualCreator = creator.get();
                User actualReceiver = receiver.get();
                List<User> users=new ArrayList<>();
                users.add(actualCreator);
                users.add(actualReceiver);
                DirectMessage directMessage=new DirectMessage(LocalDateTime.now(), users, actualCreator, true, LocalDateTime.now());
                try{
                    directMessageRepository.save(directMessage);
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
    }

    public DirectMessageDTO getChatBetweenTwoUsersByUsersId(Long userId1,Long userId2){
        Optional<DirectMessage> directMessage = directMessageRepository.findBetweenTwoUsers(userId1,userId2);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            return null;
        }
    }

    public DirectMessageDTO getChatBetweenTwoUsersByChatBoxId(Long id){
        Optional<DirectMessage> directMessage= directMessageRepository.findById(id);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            return null;
        }
    }
}
