package com.fcmobtourn.repository;
import com.fcmobtourn.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MatchRepository extends JpaRepository<Match,String> {
 List<Match> findByTournamentIdOrderByMatchdayAscIdAsc(Long tournamentId);
 List<Match> findByTournamentIdAndStage(Long tournamentId,String stage);
 void deleteByTournamentIdAndStageNot(Long tournamentId,String stage);
}
