package com.unitrovee.school;

import com.unitrovee.school.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface at runtime
 */
public interface SchoolRepository extends JpaRepository<School, Long> {

    // find a school by its email domain
    Optional<School> findByEmailDomain(String emailDomain);

    // return only active schools, one page at a time
    Page<School> findByActiveTrue(Pageable pageable);
}
