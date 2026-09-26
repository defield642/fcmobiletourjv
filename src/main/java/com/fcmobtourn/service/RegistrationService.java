package com.fcmobtourn.service;

import com.fcmobtourn.entity.User;
import com.fcmobtourn.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final TournamentService tournaments;

    public RegistrationService(UserRepository users, TournamentService tournaments) {
        this.users = users;
        this.tournaments = tournaments;
    }

    public Map<String, Object> registerUser(String name, String location, String uid) {
        if (users.countByStatus("ACTIVE") >= 8) {
            throw new IllegalStateException("Registration is full: this tournament has exactly 8 places.");
        }
        String username = name == null ? "" : name.trim();
        String playerLocation = location == null ? "" : location.trim();
        String normalizedUid = uid == null || uid.isBlank() ? null : uid.trim();
        if (username.isBlank()) throw new IllegalArgumentException("Username is required.");
        if (playerLocation.isBlank()) throw new IllegalArgumentException("Location is required.");
        if (users.findByUsername(username).isPresent()
                || (normalizedUid != null && users.findByUid(normalizedUid).isPresent())) {
            throw new IllegalArgumentException("Username or UID is already registered.");
        }

        User user = User.builder()
                .username(username)
                .location(playerLocation)
                .uid(normalizedUid == null ? "AUTO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20) : normalizedUid)
                .registrationTime(LocalDateTime.now())
                .status("ACTIVE")
                .build();
        user = users.save(user);
        tournaments.ensureLeagueFixtures();

        Map<String, Object> responseUser = new LinkedHashMap<>();
        responseUser.put("id", user.getId());
        responseUser.put("username", user.getUsername());
        responseUser.put("location", user.getLocation());
        responseUser.put("uid", user.getUid());
        responseUser.put("pot", user.getPot());
        return Map.of("success", true, "user", responseUser, "message", "Registration successful.");
    }

    public User updateUserProfile(Long id, String name) {
        User user = users.findById(id).orElseThrow();
        if (name != null && !name.isBlank()) user.setUsername(name.trim());
        return users.save(user);
    }
}