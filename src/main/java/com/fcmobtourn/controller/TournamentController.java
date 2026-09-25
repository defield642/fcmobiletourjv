package com.fcmobtourn.controller;

import com.fcmobtourn.entity.*;
import com.fcmobtourn.repository.*;
import com.fcmobtourn.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api")
public class TournamentController {
 private final UserRepository users; private final TournamentRepository tournaments; private final MatchRepository matches; private final RegistrationService registration; private final MatchService matchService;
 @Value("${admin.access.key:}") private String adminKey;
 public TournamentController(UserRepository u,TournamentRepository t,MatchRepository m,RegistrationService r,MatchService ms){users=u;tournaments=t;matches=m;registration=r;matchService=ms;}
 @GetMapping("/tournament") public Map<String,Object> state(){ Tournament t=tournaments.findTopByOrderByCreatedAtDesc().orElseGet(()->{Tournament n=Tournament.builder().status("REGISTRATION_OPEN").createdAt(java.time.LocalDateTime.now()).build();return tournaments.save(n);}); return Map.of("tournament",t,"users",users.findAll(),"matches",matches.findByTournamentIdOrderByMatchdayAscIdAsc(t.getId())); }
 @GetMapping("/users") public List<User> users(){return users.findAll();}
 @PostMapping(value="/users/register",consumes="multipart/form-data") public ResponseEntity<?> register(@RequestParam(value="username",required=false) String username,@RequestParam(value="location",required=false) String location,@RequestParam(value="uid",required=false) String uid,@RequestParam(value="squadScreenshot",required=false) MultipartFile squadScreenshot){ try { if(username==null||username.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","username is missing")); if(location==null||location.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","location is missing")); if(squadScreenshot==null||squadScreenshot.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","squadScreenshot is missing")); return ResponseEntity.ok(registration.registerUser(username,location,uid,squadScreenshot)); } catch (Exception e) { Throwable cause=e; while(cause.getCause()!=null) cause=cause.getCause(); String message=cause.getMessage()==null?e.getClass().getSimpleName():cause.getMessage(); return ResponseEntity.status(500).body(Map.of("error","Registration failed: "+message)); } }
 @PutMapping(value="/users/{id}",consumes="multipart/form-data") public ResponseEntity<?> edit(@PathVariable Long id,@RequestParam(value="username",required=false) String username,@RequestPart(value="profilePicture",required=false) MultipartFile profilePicture){return ResponseEntity.ok(registration.updateUserProfile(id,username,profilePicture));}
 @PostMapping(value="/matches/{id}/result",consumes="multipart/form-data") public ResponseEntity<?> result(@PathVariable String id,@RequestPart("screenshot") MultipartFile screenshot){return ResponseEntity.ok(matchService.uploadAndAnalyzeResult(id,screenshot));}
 @GetMapping("/admin/verify") public ResponseEntity<?> verify(@RequestHeader(value="X-Admin-Key",required=false) String key){ return authorized(key)?ResponseEntity.ok(Map.of("authorized",true)):ResponseEntity.status(401).body(Map.of("error","Invalid admin key")); }
 @PostMapping("/admin/score-error") public ResponseEntity<?> scoreError(@RequestHeader(value="X-Admin-Key",required=false) String key,@RequestBody Map<String,String> body){ if(!authorized(key)) return ResponseEntity.status(401).body(Map.of("error","Invalid admin key")); String message=body.getOrDefault("message","").trim(); if(message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","message is required")); return ResponseEntity.ok(Map.of("success",true,"message","Score error received")); }
 @PostMapping("/admin/{action}") public ResponseEntity<?> admin(@PathVariable String action,@RequestHeader(value="X-Admin-Key",required=false) String key){ if(!authorized(key)) return ResponseEntity.status(401).body(Map.of("error","Invalid admin key")); Tournament t=tournaments.findTopByOrderByCreatedAtDesc().orElseThrow(); if(action.equals("close-registrations"))t.setStatus("REGISTRATION_CLOSED"); else if(action.equals("start-tournament")){t.setStatus("IN_PROGRESS");t.setStartedAt(java.time.LocalDateTime.now());} tournaments.save(t); return ResponseEntity.ok(t); }
 @PostMapping("/matches/{id}/score") public ResponseEntity<?> score(@PathVariable String id,@RequestHeader(value="X-Admin-Key",required=false) String key,@RequestBody Map<String,Integer> body){ if(!authorized(key)) return ResponseEntity.status(401).body(Map.of("error","Invalid admin key"));return ResponseEntity.ok(matchService.updateMatchResult(id,body.get("homeScore"),body.get("awayScore")));}
 private boolean authorized(String key){ return adminKey!=null && !adminKey.isBlank() && adminKey.equals(key); }
 @ExceptionHandler(Exception.class) public ResponseEntity<?> apiError(Exception e){ Throwable cause=e; while(cause.getCause()!=null) cause=cause.getCause(); String message=cause.getMessage()==null?e.getClass().getSimpleName():cause.getMessage(); return ResponseEntity.status(500).body(Map.of("error","API request failed: "+message));}
}
