package com.fcmobtourn.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {
 @Id private String id;
 @Column(nullable=false) private Long tournamentId;
 @Column(nullable=false) private Long homeUserId, awayUserId;
 private Integer homeScore, awayScore, matchday;
 @Column(nullable=false) private String stage, status;
 @Lob @Column(columnDefinition="TEXT") private String screenshot;
 private Long winnerUserId;
 private boolean draw;
}
