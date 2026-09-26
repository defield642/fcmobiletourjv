package com.fcmobtourn.service;

import com.fcmobtourn.entity.Match;
import com.fcmobtourn.entity.Tournament;
import com.fcmobtourn.entity.User;
import com.fcmobtourn.repository.MatchRepository;
import com.fcmobtourn.repository.TournamentRepository;
import com.fcmobtourn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TournamentService {
    public static final int PLAYER_LIMIT = 8;
    private final TournamentRepository tournaments;
    private final MatchRepository matches;
    private final UserRepository users;

    public TournamentService(TournamentRepository tournaments, MatchRepository matches, UserRepository users) {
        this.tournaments = tournaments;
        this.matches = matches;
        this.users = users;
    }

    public Tournament current() {
        return tournaments.findTopByOrderByCreatedAtDesc().orElseGet(() ->
                tournaments.save(Tournament.builder()
                        .status("REGISTRATION_OPEN")
                        .createdAt(LocalDateTime.now())
                        .minUsers(PLAYER_LIMIT)
                        .maxUsers(PLAYER_LIMIT)
                        .waitingHours(0)
                        .build()));
    }

    @Transactional
    public void ensureLeagueFixtures() {
        Tournament tournament = current();
        List<User> active = orderedPlayers();
        if (active.size() != PLAYER_LIMIT
                || !matches.findByTournamentIdAndStage(tournament.getId(), "LEAGUE").isEmpty()) {
            return;
        }

        for (int i = 0; i < active.size(); i++) {
            active.get(i).setPot(i < PLAYER_LIMIT / 2 ? 1 : 2);
            users.save(active.get(i));
        }

        List<Long> rotation = active.stream().map(User::getId).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int round = 1; round <= 7; round++) {
            for (int i = 0; i < PLAYER_LIMIT / 2; i++) {
                Long home = rotation.get(i);
                Long away = rotation.get(PLAYER_LIMIT - 1 - i);
                if (round % 2 == 0) {
                    Long swap = home;
                    home = away;
                    away = swap;
                }
                matches.save(Match.builder()
                        .id("md-" + round + "-" + (i + 1))
                        .tournamentId(tournament.getId())
                        .homeUserId(home)
                        .awayUserId(away)
                        .matchday(round)
                        .stage("LEAGUE")
                        .status("PENDING")
                        .draw(false)
                        .build());
            }
            Long last = rotation.remove(rotation.size() - 1);
            rotation.add(1, last);
        }
        tournament.setStatus("IN_PROGRESS");
        tournament.setFixtureGeneratedAt(LocalDateTime.now());
        tournaments.save(tournament);
    }

    private List<User> orderedPlayers() {
        return users.findByStatus("ACTIVE").stream()
                .sorted(Comparator.comparing(User::getRegistrationTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(User::getId))
                .limit(PLAYER_LIMIT)
                .toList();
    }

    @Transactional
    public void advanceAfterScore(Tournament tournament) {
        List<Match> league = matches.findByTournamentIdAndStage(tournament.getId(), "LEAGUE");
        if (league.size() == 28 && league.stream().allMatch(this::played)
                && matches.findByTournamentIdAndStage(tournament.getId(), "SEMIFINAL").isEmpty()) {
            List<Standing> standings = standings(tournament.getId());
            createKnockoutMatch(tournament, "semi-1", standings.get(0).getUserId(), standings.get(3).getUserId());
            createKnockoutMatch(tournament, "semi-2", standings.get(1).getUserId(), standings.get(2).getUserId());
        }

        List<Match> semifinals = matches.findByTournamentIdAndStage(tournament.getId(), "SEMIFINAL");
        if (semifinals.size() == 2 && semifinals.stream().allMatch(this::played)
                && matches.findByTournamentIdAndStage(tournament.getId(), "FINAL").isEmpty()) {
            createKnockoutMatch(tournament, "final", winner(semifinals.get(0)), winner(semifinals.get(1)));
            tournament.setStatus("FINAL");
            tournaments.save(tournament);
        }

        List<Match> finalMatch = matches.findByTournamentIdAndStage(tournament.getId(), "FINAL");
        if (finalMatch.size() == 1 && played(finalMatch.get(0))) {
            tournament.setStatus("COMPLETE");
            tournaments.save(tournament);
        }
    }

    @Transactional
    public void resetKnockouts(Tournament tournament) {
        matches.deleteByTournamentIdAndStageNot(tournament.getId(), "LEAGUE");
        if ("FINAL".equals(tournament.getStatus()) || "COMPLETE".equals(tournament.getStatus())) {
            tournament.setStatus("IN_PROGRESS");
            tournaments.save(tournament);
        }
    }

    @Transactional
    public void resetFinal(Tournament tournament) {
        matches.findByTournamentIdAndStage(tournament.getId(), "FINAL").forEach(matches::delete);
        if ("FINAL".equals(tournament.getStatus()) || "COMPLETE".equals(tournament.getStatus())) {
            tournament.setStatus("IN_PROGRESS");
            tournaments.save(tournament);
        }
    }

    public List<Standing> standings(Long tournamentId) {
        Map<Long, Standing> table = new HashMap<>();
        users.findByStatus("ACTIVE").forEach(u -> table.put(u.getId(), new Standing(u.getId(), u.getUsername())));
        for (Match match : matches.findByTournamentIdAndStage(tournamentId, "LEAGUE")) {
            if (!played(match)) continue;
            Standing home = table.get(match.getHomeUserId());
            Standing away = table.get(match.getAwayUserId());
            if (home == null || away == null) continue;
            home.played++;
            away.played++;
            home.goalsFor += match.getHomeScore();
            home.goalsAgainst += match.getAwayScore();
            away.goalsFor += match.getAwayScore();
            away.goalsAgainst += match.getHomeScore();
            if (match.isDraw()) {
                home.draws++;
                away.draws++;
                home.points++;
                away.points++;
            } else if (match.getHomeScore() > match.getAwayScore()) {
                home.wins++;
                away.losses++;
                home.points += 3;
            } else {
                away.wins++;
                home.losses++;
                away.points += 3;
            }
        }
        return table.values().stream()
                .sorted(Comparator.comparingInt(Standing::getPoints).reversed()
                        .thenComparing(Comparator.comparingInt(Standing::goalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(Standing::getGoalsFor).reversed())
                        .thenComparing(Standing::getUsername))
                .toList();
    }

    private void createKnockoutMatch(Tournament tournament, String id, Long home, Long away) {
        matches.save(Match.builder()
                .id(id)
                .tournamentId(tournament.getId())
                .homeUserId(home)
                .awayUserId(away)
                .stage(id.startsWith("semi") ? "SEMIFINAL" : "FINAL")
                .status("PENDING")
                .draw(false)
                .build());
    }

    private boolean played(Match match) {
        return "PLAYED".equals(match.getStatus())
                && match.getHomeScore() != null
                && match.getAwayScore() != null;
    }

    private Long winner(Match match) {
        return match.getHomeScore() > match.getAwayScore()
                ? match.getHomeUserId() : match.getAwayUserId();
    }

    public static final class Standing {
        private final Long userId;
        private final String username;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int points;
        private int goalsFor;
        private int goalsAgainst;

        private Standing(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public int getPlayed() { return played; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public int getPoints() { return points; }
        public int getGoalsFor() { return goalsFor; }
        public int getGoalsAgainst() { return goalsAgainst; }
        int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }
}