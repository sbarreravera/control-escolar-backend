package com.graduacionesisamar.controlescolar.communication.dto;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationAudienceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CommunicationAudienceRequest(
        @NotNull @Positive Long schoolId,
        @NotNull CommunicationAudienceType audienceType,
        @Positive Long academicCycleId,
        @Size(max = 30) List<String> gradeNames,
        @Size(max = 100) List<@Positive Long> schoolGroupIds,
        @Size(max = 2000) List<@Positive Long> studentIds
) {
}
