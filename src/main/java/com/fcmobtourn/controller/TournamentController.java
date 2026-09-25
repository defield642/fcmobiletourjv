package com.fcmobtourn.controller;

import com.fcmobtourn.entity.*;
import com.fcmobtourn.repository.*;
import com.fcmobtourn.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api")
public class TournamentController {
 private final UserRepository users; private final TournamentRepository tournaments; private final MatchRepository matches; private final RegistrationService registration; private final MatchService matchService;
 public TournamentController(UserRepository u,TournamentRepository t,MatchRepository m,RegistrationService r,MatchService ms){users=u;tournaments=t;matches=m;registration=r;matchService=ms;}
 @GetMapping("/tournament") public Map<String,Object> state(){ Tournament t=tournaments.findTopByOrderByCreatedAtDesc().orElseGet(()->{Tournament n=Tournament.builder().status("REGISTRATION_OPEN").createdAt(java.time.LocalDateTime.now()).build();return tournaments.save(n);}); return Map.of("tournament",t,"users",users.findAll(),"matches",matches.findByTournamentIdOrderByMatchdayAscIdAsc(t.getId())); }
 @GetMapping("/users") public List<User> users(){return users.findAll();}
 @PostMapping(value="/users/register",consumes="multipart/form-data") public ResponseEntity<?> register(@RequestParam String username,@RequestParam String location,@RequestParam(required=false) String uid,@RequestPart(required=false) MultipartFile squadScreenshot){ return ResponseEntity.ok(registration.registerUser(username,location,uid,squadScreenshot)); }
 @PutMapping(value="/users/{id}",consumes="multipart/form-data") public ResponseEntity<?> edit(@PathVariable Long id,@RequestParam(required=false) String username,@RequestPart(required=false) MultipartFile profilePicture){return ResponseEntity.ok(registration.updateUserProfile(id,username,profilePicture));}
 @PostMapping(value="/matches/{id}/result",consumes="multipart/form-data") public ResponseEntity<?> result(@PathVariable String id,@RequestPart MultipartFile screenshot){return ResponseEntity.ok(matchService.uploadAndAnalyzeResult(id,screenshot));}
 @PostMapping("/admin/score-error") public ResponseEntity<?> scoreError(@RequestBody Map<String,String> body){ String message=body.getOrDefault("message","").trim(); if(message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","message is required")); return ResponseEntity.ok(Map.of("success",true,"message","Score error received")); }
 @PostMapping("/admin/{action}") public ResponseEntity<?> admin(@PathVariable String action){ Tournament t=tournaments.findTopByOrderByCreatedAtDesc().orElseThrow(); if(action.equals("close-registrations"))t.setStatus("REGISTRATION_CLOSED"); else if(action.equals("start-tournament")){t.setStatus("IN_PROGRESS");t.setStartedAt(java.time.LocalDateTime.now());} tournaments.save(t); return ResponseEntity.ok(t); }
 @PostMapping("/matches/{id}/score") public ResponseEntity<?> score(@PathVariable String id,@RequestBody Map<String,Integer> body){return ResponseEntity.ok(matchService.updateMatchResult(id,body.get("homeScore"),body.get("awayScore")));}
}
