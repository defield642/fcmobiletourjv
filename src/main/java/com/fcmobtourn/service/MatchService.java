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
        Tournament tournament = tournaments.findById(match.getTournamentId()).orElseThrow();
        if ("LEAGUE".equals(match.getStage())) {
            tournamentService.resetKnockouts(tournament);
        } else {
            tournamentService.resetStagesAfter(tournament, match.getStage());
        }
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus("PLAYED");
        match.setDraw(homeScore.equals(awayScore));
        match.setWinnerUserId(homeScore > awayScore ? match.getHomeUserId()
                : awayScore > homeScore ? match.getAwayUserId() : null);
        if ("FINAL".equals(match.getStage()) && homeScore.equals(awayScore)) {
            throw new IllegalArgumentException("The final cannot finish level.");
        }
        Match saved = matches.save(match);
        if (match.getSeriesId() != null && !"FINAL".equals(match.getStage())
                && matches.findByTournamentIdAndSeriesId(match.getTournamentId(), match.getSeriesId()).stream().allMatch(m ->
                "PLAYED".equals(m.getStatus()) && m.getHomeScore() != null && m.getAwayScore() != null)) {
            if (tournamentService.seriesWinner(tournament, match.getSeriesId()) == null) {
                throw new IllegalArgumentException("Aggregate score is level. Edit a leg so the aggregate winner is clear.");
            }
        }
        tournamentService.advanceAfterScore(tournament);
        return saved;
    }
}
