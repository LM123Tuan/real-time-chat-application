package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "chatbox")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ChatBox {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(nullable = false)
    private LocalDateTime createTime;
    @ManyToMany
    @JoinTable(
            name = "chatbox_user",
            joinColumns = @JoinColumn(name = "chatbox_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatboxType chatboxType;
    @Column(nullable = false)
    private boolean isActive;
    @Column(nullable = false)
    private LocalDateTime lastActiveTime;

    public ChatBox() {}
    public ChatBox(String name, LocalDateTime createTime, Set<User> users, ChatboxType chatboxType, boolean isActive, LocalDateTime lastActiveTime) {
        this.name=name;
        this.createTime = createTime;
        this.users = users;
        this.chatboxType = chatboxType;
        this.isActive = isActive;
        this.lastActiveTime = lastActiveTime;
    }

    public Long getId(){
        return id;
    }
    public String getName() {return name;}
    public LocalDateTime getCreateTime(){
        return createTime;
    }
    public Set<User> getUsers(){
        return users;
    }
    public ChatboxType getChatboxType(){
        return chatboxType;
    }
    public boolean isActive(){
        return isActive;
    }
    public LocalDateTime getLastActiveTime(){
        return lastActiveTime;
    }

    public void setName(String name) {this.name=name;}
    public void setCreateTime(LocalDateTime createTime){
        this.createTime = createTime;
    }
    public void setUsers(Set<User> users){
        this.users = users;
    }
    public void setChatboxType(ChatboxType chatboxType){
        this.chatboxType = chatboxType;
    }
    public void setActive(boolean isActive){
        this.isActive = isActive;
    }
    public void setLastActiveTime(LocalDateTime lastActiveTime){
        this.lastActiveTime = lastActiveTime;
    }
}
