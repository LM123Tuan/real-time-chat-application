package com.tuan.chatserver.entity;

import com.tuan.chatserver.enums.AuthProvider;
import com.tuan.chatserver.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
public class User extends Person{
    @Column(nullable = false)
    private String fullname;
    @Column()
    private String phone;
    @Column(unique = true, nullable = false)
    @Email(message = "Invalid email format!")
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;
    @Column(name = "provider_id")
    private String providerId;
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
        this(fullname, username, email, password, phone,
                isActive, AuthProvider.LOCAL, null);
    }
    public User(String fullname, String username, String email, String password, String phone, boolean isActive, AuthProvider provider, String providerId) {
        super(username, password, UserRole.USER, isActive);
        this.fullname = fullname;
        this.phone = phone;
        this.email = email;
        this.provider=provider;
        this.providerId=providerId;
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
    public AuthProvider getProvider(){
        return this.provider;
    }
    public String getProviderId(){
        return this.providerId;
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
    public void setProvider(AuthProvider provider){
        this.provider=provider;
    }
    public void setProviderId(String providerId){
        this.providerId=providerId;
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
