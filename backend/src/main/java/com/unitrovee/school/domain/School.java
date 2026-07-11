package com.unitrovee.school.domain;

import com.unitrovee.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
public class School extends BaseEntity {

    @Column(nullable = false)
    private String name;        // eg: University College Dublin

    @Column(name = "short_name", nullable = false)
    private String shortName;       // eg: UCD

    @Column(name = "email_domain", nullable = false, unique = true)
    private String emailDomain;

    private String city;

    @Column(nullable = false)
    private boolean active = true;
}
