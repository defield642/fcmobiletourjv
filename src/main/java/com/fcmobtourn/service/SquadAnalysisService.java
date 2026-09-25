package com.fcmobtourn.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@Service
public class SquadAnalysisService {
    
    private final AIService aiService;
    
    public SquadAnalysisService(AIService aiService) {
        this.aiService = aiService;
    }
    
    public Map<String, Object> analyzeSquad(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            
            // Call AI to get OVR
            Integer ovr = aiService.analyzeSquadScreenshot(base64Image);
            
            Map<String, Object> result = new HashMap<>();
            result.put("ovr", ovr);
            result.put("status", "ANALYZED");
            result.put("screenshot_url", "uploads/" + file.getOriginalFilename());
            
            return result;
        } catch (IOException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("ovr", 125);
            result.put("status", "DEFAULT");
            result.put("error", "Failed to read image: " + e.getMessage());
            return result;
        }
    }
}