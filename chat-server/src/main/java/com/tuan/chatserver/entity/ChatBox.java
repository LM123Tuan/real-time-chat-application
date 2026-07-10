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

    public ChatBox() {}
    public ChatBox(LocalDateTime createTime, User creator, ChatboxType chatboxType) {
        this.createTime = createTime;
        this.users = new ArrayList<>();
        this.creator = creator;
        this.chatboxType = chatboxType;
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
}
