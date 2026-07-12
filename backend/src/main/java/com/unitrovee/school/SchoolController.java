package com.unitrovee.school;

import com.unitrovee.common.ApiResponse;
import com.unitrovee.common.PageResponse;
import com.unitrovee.school.dto.SchoolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping
    public ApiResponse<PageResponse<SchoolResponse>> list(Pageable pageable) {
        return ApiResponse.ok(schoolService.getSchools(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<SchoolResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(schoolService.getSchoolById(id));
    }
}
