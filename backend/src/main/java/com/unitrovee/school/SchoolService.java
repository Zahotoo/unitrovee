package com.unitrovee.school;

import com.unitrovee.common.PageResponse;
import com.unitrovee.school.dto.SchoolResponse;
import org.springframework.data.domain.Pageable;

public interface SchoolService {

    // list active schools, one page at a time
    PageResponse<SchoolResponse> getSchools(Pageable pageable);

    // fetch a single school by id (404 if it doesn't exist)
    SchoolResponse getSchoolById(Long id);
}
