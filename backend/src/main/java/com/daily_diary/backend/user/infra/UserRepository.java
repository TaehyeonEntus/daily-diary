package com.daily_diary.backend.user.infra;

import com.daily_diary.backend.user.entity.User;
import com.daily_diary.backend.user.exception.UserNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    default User findOrThrow(Long id) {
        return findById(id).orElseThrow(UserNotFoundException::new);
    }

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
