package com.fcmobtourn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="tournaments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tournament {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String status;
 private LocalDateTime createdAt, registrationClosedAt, fixtureGeneratedAt, startedAt;
 private Integer minUsers=8, maxUsers=8, waitingHours=0;
}
