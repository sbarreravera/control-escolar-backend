package com.graduacionesisamar.controlescolar.student.dto;

import java.util.List;

/**
 * Represents one page of students returned by the API.
 */
public record StudentPageResponse(
        List<StudentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
