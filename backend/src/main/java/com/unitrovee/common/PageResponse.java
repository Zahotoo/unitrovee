package com.unitrovee.common;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Envelope for paginated list endpoints. Sits inside ApiResponse's "data".
 * @param content items on the current page
 * @param page current page index (0-based: first page is 0)
 * @param size page size (max item per page)
 * @param totalElements total items across ALL pages
 * @param totalPages total number of pages
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    /**
     * Factory: convert a Spring Data Page into our PageResponse. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}