package com.fcmobtourn.repository;

import com.fcmobtourn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUid(String uid);
    List<User> findByStatus(String status);
    long countByStatus(String status);
}