package com.pulse.PulsePass.domain;

import jakarta.persistence.*;

@Entity @Table(name="user_credentials")
public class UserCredential {
 @Id @Column(name="user_id") private Long userId;
 @MapsId @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 protected UserCredential() {}
 public UserCredential(User user,String hash){this.user=user;this.passwordHash=hash;}
 public String getPasswordHash(){return passwordHash;}
 public User getUser(){return user;}
}
