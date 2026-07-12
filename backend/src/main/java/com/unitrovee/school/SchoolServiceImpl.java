package com.unitrovee.school;

import com.unitrovee.common.PageResponse;
import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.school.domain.School;
import com.unitrovee.school.dto.SchoolResponse;
import com.unitrovee.school.mapper.SchoolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    @Override
    public PageResponse<SchoolResponse> getSchools(Pageable pageable) {
        // fetch one page of ACTIVE schools from DB
        Page<School> schools = schoolRepository.findByActiveTrue(pageable);
        // map each school entity -> SchoolResponse DTO
        Page<SchoolResponse> mapped = schools.map(schoolMapper::toResponse);
        // convert Spring's page into our own PageResponse envelops
        return PageResponse.from(mapped);
    }

    @Override
    public SchoolResponse getSchoolById(Long id) {
        School school = schoolRepository.findById(id)
                // not found -> throw GlobalExceptionHandler turns it into a 404 envelope
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + id));

        return schoolMapper.toResponse(school);
    }
}
