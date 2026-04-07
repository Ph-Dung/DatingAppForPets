package com.petmatch.backend.repository;


import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    long countByIsLockedTrue();

    @Query("""
        SELECT u FROM User u
        WHERE u.role = :role
          AND (:locked IS NULL OR u.isLocked = :locked)
                    AND (:warned IS NULL OR u.isWarned = :warned)
          AND (:query IS NULL OR :query = ''
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY u.createdAt DESC
        """)
    Page<User> searchUsersForAdmin(@Param("role") Role role,
                                   @Param("query") String query,
                                   @Param("locked") Boolean locked,
                                                                     @Param("warned") Boolean warned,
                                   Pageable pageable);
}
