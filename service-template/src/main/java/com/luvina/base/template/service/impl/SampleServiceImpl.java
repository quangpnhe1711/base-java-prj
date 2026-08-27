package com.luvina.base.template.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luvina.base.core.dto.DeleteBaseRequest;
import com.luvina.base.core.exception.DuplicateResourceException;
import com.luvina.base.core.exception.ResourceNotFoundException;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageUtil;
import com.luvina.base.core.locking.OptimisticLockSupport;
import com.luvina.base.core.util.SqlUtils;
import com.luvina.base.core.util.StringUtil;
import com.luvina.base.template.constant.TemplateMessageKey;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.request.SampleSearchRequest;
import com.luvina.base.template.dto.request.SampleUpdateRequest;
import com.luvina.base.template.dto.response.SamplePageResponse;
import com.luvina.base.template.dto.response.SampleResponse;
import com.luvina.base.template.entity.Sample;
import com.luvina.base.template.mapper.SampleMapper;
import com.luvina.base.template.projection.SampleListProjection;
import com.luvina.base.template.repository.SampleRepository;
import com.luvina.base.template.service.SampleService;

import lombok.RequiredArgsConstructor;

/**
 * Reference implementation of the sample use cases.
 *
 * <p>It demonstrates the conventions a new service is expected to follow:
 * <ul>
 *   <li>constructor injection through {@code @RequiredArgsConstructor}, never
 *       field injection;</li>
 *   <li>read methods marked {@code @Transactional(readOnly = true)}, write
 *       methods {@code @Transactional};</li>
 *   <li>uniqueness checked explicitly, so the user gets a precise message
 *       instead of a database constraint error;</li>
 *   <li>writes that the client initiated from a previous read go through
 *       {@link OptimisticLockSupport};</li>
 *   <li>no {@code save} call after modifying a managed entity: the flush at
 *       commit persists it.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SampleServiceImpl implements SampleService {

    /** Columns a client is allowed to sort by, mapped to the physical column. */
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "code", "code",
            "name", "name",
            "status", "status",
            "effectiveDate", "effective_date",
            "updatedAt", "updated_at");

    private static final String DEFAULT_SORT_COLUMN = "updated_at";

    private final SampleRepository sampleRepository;
    private final SampleMapper sampleMapper;
    private final OptimisticLockSupport lockSupport;
    private final MessageUtil messageUtil;

    @Override
    @Transactional(readOnly = true)
    public SamplePageResponse search(SampleSearchRequest request) {
        Page<SampleListProjection> page = sampleRepository.search(
                // Escaped so that a user typing "%" searches for a percent sign
                // rather than matching every row.
                SqlUtils.escapeLike(StringUtil.blankToNull(request.getCode())),
                SqlUtils.escapeLike(StringUtil.blankToNull(request.getName())),
                request.getStatus() == null ? null : request.getStatus().name(),
                toPageable(request));

        SamplePageResponse response = new SamplePageResponse();
        response.setSampleList(sampleMapper.toResponseList(page.getContent()));
        response.applyPageMetadata(page);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleResponse getById(UUID id) {
        return sampleMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public SampleResponse create(SampleCreateRequest request) {
        if (sampleRepository.existsByCodeAndDeleteFlagFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    messageUtil.format(ErrorCode.ERR002, TemplateMessageKey.SAMPLE_CODE));
        }
        // saveAndFlush, not save: the audit timestamps are assigned by Hibernate
        // at flush time, and updatedAt has to be in the response or the client
        // cannot perform its next update without re-reading the record.
        Sample saved = sampleRepository.saveAndFlush(sampleMapper.toEntity(request));
        return sampleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SampleResponse update(UUID id, SampleUpdateRequest request) {
        Sample sample = lockSupport.loadForUpdate(
                sampleRepository, id, request.getUpdatedAt(), TemplateMessageKey.SAMPLE);

        sampleMapper.update(request, sample);
        // The entity is managed, so the change would reach the database at commit
        // on its own. Flushing early is still required: updatedAt is assigned by
        // Hibernate at flush, and the response has to carry the new token or the
        // client's next update would be rejected as stale.
        sampleRepository.flush();
        return sampleMapper.toResponse(sample);
    }

    @Override
    @Transactional
    public void delete(DeleteBaseRequest request) {
        lockSupport.softDelete(
                sampleRepository, request.getId(), request.getUpdatedAt(), TemplateMessageKey.SAMPLE);
    }

    private Sample findOrThrow(UUID id) {
        return sampleRepository.findByIdAndDeleteFlagFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageUtil.format(ErrorCode.ERR003, TemplateMessageKey.SAMPLE)));
    }

    /**
     * Builds the {@code Pageable}, translating the requested sort field to a
     * physical column.
     *
     * <p>The allow-list matters: the column name ends up inside the generated
     * ORDER BY, so accepting an arbitrary string from the client would be an
     * injection point.
     */
    private Pageable toPageable(SampleSearchRequest request) {
        // Map.of rejects a null key, and orderBy is optional.
        String requested = request.getOrderBy();
        String column = requested == null
                ? DEFAULT_SORT_COLUMN
                : SORTABLE_COLUMNS.getOrDefault(requested, DEFAULT_SORT_COLUMN);
        Sort.Direction direction = request.getOrderType() == null
                ? Sort.Direction.ASC
                : request.getOrderType().direction();
        Sort sort = Sort.by(direction, column);
        return PageRequest.of(request.zeroBasedPage(), request.effectivePageSize(), sort);
    }
}
