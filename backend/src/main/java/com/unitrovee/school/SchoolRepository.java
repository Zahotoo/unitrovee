package com.unitrovee.school;

import com.unitrovee.school.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface at runtime
 */
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByEmailDomain(String emailDomain);
}
