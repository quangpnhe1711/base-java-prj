package com.luvina.base.template.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luvina.base.core.util.DateTimeUtil;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.request.SampleUpdateRequest;
import com.luvina.base.template.dto.response.SampleResponse;
import com.luvina.base.template.entity.Sample;
import com.luvina.base.template.projection.SampleListProjection;

/**
 * Converts between the sample entity, its DTOs and its projection.
 *
 * <p>MapStruct generates the implementation at compile time, so a field that
 * cannot be mapped is a build failure rather than a silent null at runtime. The
 * build sets {@code unmappedTargetPolicy=ERROR}, which is why every target field
 * that is intentionally left alone is listed as an explicit {@code ignore}.
 * Adding a field to the entity therefore forces a decision here.
 */
@Mapper
public interface SampleMapper {

    /**
     * Builds a new entity from a create request.
     *
     * <p>Identity and audit columns are filled by JPA, never by the caller.
     *
     * @param request create payload
     * @return unsaved entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedUserId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleteFlag", ignore = true)
    Sample toEntity(SampleCreateRequest request);

    /**
     * Applies an update to a managed entity.
     *
     * <p>{@code code} stays untouched: the business identifier is not editable.
     * {@code updatedAt} is the lock token that was already verified; copying it
     * onto the entity would overwrite the value Hibernate is about to set.
     *
     * @param request update payload
     * @param entity  managed entity to modify in place
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedUserId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleteFlag", ignore = true)
    void update(SampleUpdateRequest request, @MappingTarget Sample entity);

    /**
     * Converts an entity to its API representation.
     *
     * @param entity persisted entity
     * @return response payload
     */
    SampleResponse toResponse(Sample entity);

    /**
     * Converts a search projection to the API representation.
     *
     * <p>The list view does not select the description, so the field stays null.
     *
     * @param projection search row
     * @return response payload
     */
    @Mapping(target = "description", ignore = true)
    SampleResponse toResponse(SampleListProjection projection);

    /**
     * Converts a page of search projections.
     *
     * @param projections search rows
     * @return response payloads
     */
    List<SampleResponse> toResponseList(List<SampleListProjection> projections);

    /**
     * Bridges the timestamp types: native queries return {@link Instant} while
     * the API speaks {@link OffsetDateTime}.
     *
     * @param value timestamp from a projection, may be null
     * @return the same moment in the application time zone
     */
    default OffsetDateTime toOffsetDateTime(Instant value) {
        return DateTimeUtil.toOffsetDateTime(value);
    }
}
