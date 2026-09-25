package com.fcmobtourn.service;
import com.fcmobtourn.entity.User; import com.fcmobtourn.repository.UserRepository; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile; import java.time.LocalDateTime; import java.util.*;
@Service public class RegistrationService {
 private final UserRepository repo; private final AIService ai; private final JdbcTemplate jdbc;
 public RegistrationService(UserRepository r,AIService a,JdbcTemplate j){repo=r;ai=a;jdbc=j;}
 public Map<String,Object> registerUser(String name,String location,String uid,MultipartFile squad){ makeUidOptionalForExistingDatabase(); if(repo.countByStatus("ACTIVE")>=20) throw new IllegalStateException("Registration is full: maximum 20 members"); String normalizedUid=uid==null||uid.isBlank()?null:uid.trim(); if(repo.findByUsername(name).isPresent()||(normalizedUid!=null&&repo.findByUid(normalizedUid).isPresent())) throw new IllegalArgumentException("Username or UID already registered"); User u=User.builder().username(name).location(location).uid(normalizedUid).registrationTime(LocalDateTime.now()).status("ACTIVE").squadOvr(125).build(); try{if(squad!=null&&!squad.isEmpty()){String data=Base64.getEncoder().encodeToString(squad.getBytes());u.setSquadScreenshot(data);u.setSquadOvr(ai.analyzeSquadScreenshot(data));}}catch(Exception ignored){} u=repo.save(u);return Map.of("success",true,"user",u,"message","Registration successful"); }
 private void makeUidOptionalForExistingDatabase(){
  try{ jdbc.execute("ALTER TABLE users ALTER COLUMN uid DROP NOT NULL"); }catch(Exception ignored){}
 }
 public Map<String,Object> updateUserProfile(Long id,String name,MultipartFile picture){User u=repo.findById(id).orElseThrow();if(name!=null&&!name.isBlank())u.setUsername(name);try{if(picture!=null&&!picture.isEmpty())u.setProfilePicture(Base64.getEncoder().encodeToString(picture.getBytes()));}catch(Exception ignored){}repo.save(u);return Map.of("success",true,"user",u);}
}
