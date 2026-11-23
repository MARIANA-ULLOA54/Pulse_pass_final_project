package com.pulse.PulsePass.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
@Entity @Table(name="user_roles") @IdClass(UserRole.Key.class)
public class UserRole {
 @Id @Column(name="user_id") private Long userId;
 @Id @Column(name="role",length=10) private String role;
 protected UserRole() {}
 public UserRole(Long userId,String role){this.userId=userId;this.role=role;}
 public String getRole(){return role;}
 public static class Key implements Serializable {
  public Long userId; public String role;
  public Key(){} public boolean equals(Object o){return o instanceof Key k && Objects.equals(userId,k.userId)&&Objects.equals(role,k.role);}
  public int hashCode(){return Objects.hash(userId,role);
  }
 }
}
