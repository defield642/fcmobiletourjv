package com.fcmobtourn.controller;

import com.fcmobtourn.entity.Match;
import com.fcmobtourn.entity.Tournament;
import com.fcmobtourn.entity.User;
import com.fcmobtourn.repository.MatchRepository;
import com.fcmobtourn.repository.TournamentRepository;
import com.fcmobtourn.repository.UserRepository;
import com.fcmobtourn.service.MatchService;
import com.fcmobtourn.service.RegistrationService;
import com.fcmobtourn.service.TournamentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TournamentController {
    private final UserRepository users;
    private final TournamentRepository tournaments;
    private final MatchRepository matches;
    private final RegistrationService registration;
    private final MatchService matchService;
    private final TournamentService tournamentService;

    @Value("${admin.username:Timmy45G}")
    private String configuredAdminUsername;

    @Value("${admin.access.pin:1234}")
    private String configuredAdminPin;

    public TournamentController(UserRepository users, TournamentRepository tournaments, MatchRepository matches,
                                RegistrationService registration, MatchService matchService,
                                TournamentService tournamentService) {
        this.users = users;
        this.tournaments = tournaments;
        this.matches = matches;
        this.registration = registration;
        this.matchService = matchService;
        this.tournamentService = tournamentService;
    }

    @GetMapping("/tournament")
    public Map<String, Object> state() {
        tournamentService.ensureLeagueFixtures();
        Tournament tournament = tournamentService.current();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tournament", tournament);
        response.put("users", users.findByStatus("ACTIVE"));
        response.put("matches", matches.findByTournamentIdOrderByMatchdayAscIdAsc(tournament.getId()));
        response.put("standings", tournamentService.standings(tournament.getId()));
        return response;
    }

    @GetMapping("/users")
    public List<User> users() {
        return users.findByStatus("ACTIVE");
    }

    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String location,
                                      @RequestParam(required = false) String uid) {
        try {
            return ResponseEntity.ok(registration.registerUser(username, location, uid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> edit(@PathVariable Long id, @RequestParam(required = false) String username) {
        try {
            return ResponseEntity.ok(registration.updateUserProfile(id, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        if (authorized(credentials.get("username"), credentials.get("pin"))) {
            return ResponseEntity.ok(Map.of("authorized", true, "username", configuredAdminUsername));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid administrator name or PIN."));
    }

    @GetMapping("/admin/verify")
    public ResponseEntity<?> verify(@RequestHeader(value = "X-Admin-User", required = false) String username,
                                    @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        return authorized(username, pin)
                ? ResponseEntity.ok(Map.of("authorized", true))
                : ResponseEntity.status(401).body(Map.of("error", "Invalid administrator name or PIN."));
    }

    @PostMapping("/admin/matches/{id}/score")
    public ResponseEntity<?> score(@PathVariable String id,
                                   @RequestHeader(value = "X-Admin-User", required = false) String username,
                                   @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                   @RequestBody Map<String, Integer> body) {
        if (!authorized(username, pin)) {
            return ResponseEntity.status(401).body(Map.of("error", "Administrator access required."));
        }
        try {
            return ResponseEntity.ok(matchService.updateMatchResult(id, body.get("homeScore"), body.get("awayScore")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean authorized(String username, String pin) {
        return configuredAdminUsername != null && configuredAdminPin != null
                && configuredAdminUsername.equals(username)
                && configuredAdminPin.length() == 4
                && configuredAdminPin.equals(pin);
    }
}