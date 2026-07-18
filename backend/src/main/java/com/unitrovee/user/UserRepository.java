package com.unitrovee.user;

import com.unitrovee.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // fetch school in the same query, so mapping stays safe inside the service transaction
    @Query("""
            select user
            from User user
            join fetch user.school
            where user.email = :email
            """)
    Optional<User> findByEmailWithSchool(@Param("email") String email);

    boolean existsByEmail(String email);
}
