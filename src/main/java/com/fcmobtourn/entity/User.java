package com.fcmobtourn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true, length=30) private String username;
 @Column(nullable=false, length=80) private String location;
 @Column(nullable=false, unique=true, length=40) private String uid;
 private Integer squadOvr;
 @Lob @Column(columnDefinition="TEXT") private String squadScreenshot;
 @Lob @Column(columnDefinition="TEXT") private String profilePicture;
 private LocalDateTime registrationTime;
 @Column(nullable=false, length=20) private String status;
}
