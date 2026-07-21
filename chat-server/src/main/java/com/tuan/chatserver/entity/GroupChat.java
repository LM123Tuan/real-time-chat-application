package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.ChatboxType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "group_chat")
public class GroupChat extends ChatBox{
    @Column(name = "name", nullable = false)
    private String name;
    @ManyToMany
    @JoinTable(
            name = "chat_box_leaders",
            joinColumns = @JoinColumn(name = "chat_box_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> leaders;
    @ManyToMany
    @JoinTable(
            name = "chat_box_vice_leaders",
            joinColumns = @JoinColumn(name = "chat_box_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> viceLeaders;
    public GroupChat() {}
    public GroupChat(LocalDateTime createTime, Set<User> users, Set<User> leaders, Set<User> viceLeaders, String name, boolean isActive, LocalDateTime lastActiveTime) {
        super(createTime, users, ChatboxType.GROUP_CHAT, isActive, lastActiveTime);
        this.name = name;
        this.viceLeaders = viceLeaders;
        this.leaders = leaders;
    }

    public String getName() {
        return name;
    }
    public Set<User> getLeaders() {
        return leaders;
    }
    public Set<User> getViceLeaders() {
        return viceLeaders;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setLeaders(Set<User> leaders) {
        this.leaders = leaders;
    }
    public void setViceLeaders(Set<User> viceLeaders) {
        this.viceLeaders = viceLeaders;
    }
}
