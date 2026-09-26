package com.fcmobtourn.service;

import com.fcmobtourn.entity.Match;
import com.fcmobtourn.entity.Tournament;
import com.fcmobtourn.repository.MatchRepository;
import com.fcmobtourn.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {
    private final MatchRepository matches;
    private final TournamentRepository tournaments;
    private final TournamentService tournamentService;

    public MatchService(MatchRepository matches, TournamentRepository tournaments, TournamentService tournamentService) {
        this.matches = matches;
        this.tournaments = tournaments;
        this.tournamentService = tournamentService;
    }

    @Transactional
    public Match updateMatchResult(String id, Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0 || homeScore > 99 || awayScore > 99) {
            throw new IllegalArgumentException("Both scores must be whole numbers from 0 to 99.");
        }
        Match match = matches.findById(id).orElseThrow(() -> new IllegalArgumentException("Match not found."));
        if (!"LEAGUE".equals(match.getStage()) && homeScore.equals(awayScore)) {
            throw new IllegalArgumentException("Knockout matches cannot finish level. Enter the winner's final score.");
        }

        Tournament tournament = tournaments.findById(match.getTournamentId()).orElseThrow();
        if ("LEAGUE".equals(match.getStage())) {
            tournamentService.resetKnockouts(tournament);
        } else if ("SEMIFINAL".equals(match.getStage())) {
            tournamentService.resetFinal(tournament);
        }
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus("PLAYED");
        match.setDraw(homeScore.equals(awayScore));
        match.setWinnerUserId(homeScore > awayScore ? match.getHomeUserId()
                : awayScore > homeScore ? match.getAwayUserId() : null);
        Match saved = matches.save(match);
        tournamentService.advanceAfterScore(tournament);
        return saved;
    }
}