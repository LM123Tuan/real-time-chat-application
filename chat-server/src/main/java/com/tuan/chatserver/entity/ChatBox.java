package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chatbox")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ChatBox {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime createTime;
    @ManyToMany
    @JoinTable(
            name = "chatbox_user",
            joinColumns = @JoinColumn(name = "chatbox_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatboxType chatboxType;
    @Column(nullable = false)
    private LocalDateTime lastActiveTime;

    public ChatBox() {}
    public ChatBox(LocalDateTime createTime, List<User> users, User creator, ChatboxType chatboxType, LocalDateTime lastActiveTime) {
        this.createTime = createTime;
        this.users = users;
        this.creator = creator;
        this.chatboxType = chatboxType;
        this.lastActiveTime = lastActiveTime;
    }

    public Long getId(){
        return id;
    }
    public LocalDateTime getCreateTime(){
        return createTime;
    }
    public List<User> getUsers(){
        return users;
    }
    public User getCreator(){
        return creator;
    }
    public ChatboxType getChatboxType(){
        return chatboxType;
    }
    public LocalDateTime getLastActiveTime(){
        return lastActiveTime;
    }

    public void setCreateTime(LocalDateTime createTime){
        this.createTime = createTime;
    }
    public void setUsers(List<User> users){
        this.users = users;
    }
    public void setCreator(User creator){
        this.creator = creator;
    }
    public void setChatboxType(ChatboxType chatboxType){
        this.chatboxType = chatboxType;
    }
    public void setLastActiveTime(LocalDateTime lastActiveTime){
        this.lastActiveTime = lastActiveTime;
    }
}
