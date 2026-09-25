package com.fcmobtourn.repository;
import com.fcmobtourn.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TournamentRepository extends JpaRepository<Tournament,Long> {
 Optional<Tournament> findTopByOrderByCreatedAtDesc();
 long countByStatus(String status);
}
