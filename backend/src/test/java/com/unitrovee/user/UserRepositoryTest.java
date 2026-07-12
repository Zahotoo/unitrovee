package com.unitrovee.user;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    SchoolRepository schoolRepository;

    @Test
    void persistsUserWithSchoolForeignKey() {

        School school = schoolRepository.findAll().get(0);

        User user = new User();
        user.setEmail("alice@ucdconnect.ie");
        user.setPasswordHash("$2a$dummy-bcrypt-hash");
        user.setDisplayName("Alice");
        user.setSchool(school);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();

        // read back and confirm the FK association resolves to the right school
        User found = userRepository.findByEmail("alice@ucdconnect.ie").orElseThrow();
        assertThat(found.getSchool().getId()).isEqualTo(school.getId());
        assertThat(found.getRole()).isEqualTo(Role.STUDENT);
        assertThat(found.isEmailVerified()).isFalse();
        assertThat(found.getReputationScore()).isZero();
    }

    @Test
    void emailMustBeUnique() {
        School school = schoolRepository.findAll().get(0);
        userRepository.saveAndFlush(newUser("dup@ucdconnect.ie", school));

        // second user with the same email must be rejected by the UNIQUE constraint
        User duplicate = newUser("dup@ucdconnect.ie", school);
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // small helper to build a valid user
    private User newUser(String email, School school) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Test");
        user.setSchool(school);
        return user;
    }
}
