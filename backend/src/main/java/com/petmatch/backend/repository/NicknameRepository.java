package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Nickname;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NicknameRepository extends JpaRepository<Nickname, Long> {
    Optional<Nickname> findBySetterAndReceiver(User setter, User receiver);
}
