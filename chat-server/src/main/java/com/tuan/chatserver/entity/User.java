package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User extends Person{
    @Column(nullable = false)
    private String fullname;
    @Column()
    private String phone;
    @Column(unique = true, nullable = false)
    private String email;
    @ManyToMany(mappedBy = "users")
    private Set<ChatBox> chatBoxes = new HashSet<>();
    @ManyToMany(mappedBy = "leaders")
    private Set<GroupChat> leaders = new HashSet<>();
    @ManyToMany(mappedBy = "viceLeaders")
    private Set<GroupChat> viceLeaders = new HashSet<>();
    public User(){
        super();
    }
    public User(String fullname, String username, String email, String password, String phone, boolean isActive) {
        super(username, password, UserRole.USER, isActive);
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }
    public String getPhone() {
        return phone;
    }
    public String getEmail() {
        return email;
    }
    public Set<ChatBox> getChatBoxes() {
        return chatBoxes;
    }
    public Set<GroupChat> getLeaders() {
        return leaders;
    }
    public Set<GroupChat> getViceLeaders() {
        return viceLeaders;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setChatBoxes(Set<ChatBox> chatBoxes) {
        this.chatBoxes = chatBoxes;
    }
    public void setLeaders(Set<GroupChat> leaders) {
        this.leaders = leaders;
    }
    public void setViceLeaders(Set<GroupChat> viceLeaders) {
        this.viceLeaders = viceLeaders;
    }
}
