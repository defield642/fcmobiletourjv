package com.fcmobtourn.service;
import com.fcmobtourn.entity.Match; import com.fcmobtourn.entity.User; import com.fcmobtourn.repository.*; import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile; import java.util.*;
@Service public class MatchService {
 private final MatchRepository matches; private final UserRepository users; private final AIService ai;
 public MatchService(MatchRepository m,UserRepository u,AIService a){matches=m;users=u;ai=a;}
 public Match updateMatchResult(String id,Integer h,Integer a){Match m=matches.findById(id).orElseThrow();m.setHomeScore(h);m.setAwayScore(a);m.setStatus("PLAYED");m.setDraw(h.equals(a));m.setWinnerUserId(h>a?m.getHomeUserId():a>h?m.getAwayUserId():null);return matches.save(m);}
 public Map<String,Object> uploadAndAnalyzeResult(String id,MultipartFile file){try{Match m=matches.findById(id).orElseThrow();User h=users.findById(m.getHomeUserId()).orElseThrow();User a=users.findById(m.getAwayUserId()).orElseThrow();String image=Base64.getEncoder().encodeToString(file.getBytes());Map<String,Object> x=ai.analyzeMatchScreenshot(image,h.getUsername(),a.getUsername());if((int)x.get("homeScore")<0 || !(Boolean)x.get("teamsMatch"))return Map.of("success",false,"message","Wrong teams or unreadable score screenshot. Re-upload the correct fixture result.");m.setScreenshot(image);updateMatchResult(id,(int)x.get("homeScore"),(int)x.get("awayScore"));return Map.of("success",true,"match",m,"analysis",x);}catch(Exception e){return Map.of("success",false,"message",e.getMessage());}}
}
