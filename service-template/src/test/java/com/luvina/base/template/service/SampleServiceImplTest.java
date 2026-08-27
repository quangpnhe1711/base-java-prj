package com.luvina.base.template.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.luvina.base.core.exception.DuplicateResourceException;
import com.luvina.base.core.exception.ResourceNotFoundException;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageKey;
import com.luvina.base.core.i18n.MessageUtil;
import com.luvina.base.core.locking.OptimisticLockSupport;
import com.luvina.base.template.constant.TemplateMessageKey;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.request.SampleSearchRequest;
import com.luvina.base.template.dto.request.SampleUpdateRequest;
import com.luvina.base.template.dto.response.SamplePageResponse;
import com.luvina.base.template.dto.response.SampleResponse;
import com.luvina.base.template.entity.Sample;
import com.luvina.base.template.enums.SampleStatus;
import com.luvina.base.template.mapper.SampleMapper;
import com.luvina.base.template.repository.SampleRepository;
import com.luvina.base.template.service.impl.SampleServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the service layer.
 *
 * <p>Collaborators are mocked, except the MapStruct mapper: it is generated
 * code, so exercising the real one also checks that the mapping annotations are
 * right. A mocked mapper would pass even with a field left unmapped.
 *
 * <p>This is the test level that should carry most of the behavioural coverage:
 * fast, no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SampleServiceImpl")
class SampleServiceImplTest {

    private static final String LOCALISED_MESSAGE = "localised message";

    @Mock
    private SampleRepository sampleRepository;

    @Mock
    private OptimisticLockSupport lockSupport;

    @Mock
    private MessageUtil messageUtil;

    private SampleMapper sampleMapper;
    private SampleServiceImpl service;

    @BeforeEach
    void setUp() {
        sampleMapper = Mappers.getMapper(SampleMapper.class);
        service = new SampleServiceImpl(sampleRepository, sampleMapper, lockSupport, messageUtil);
    }

    @Test
    @DisplayName("creates a sample when the code is free")
    void createsSample() {
        SampleCreateRequest request = createRequest();
        when(sampleRepository.existsByCodeAndDeleteFlagFalse("ABC")).thenReturn(false);
        when(sampleRepository.saveAndFlush(any(Sample.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SampleResponse response = service.create(request);

        ArgumentCaptor<Sample> saved = ArgumentCaptor.forClass(Sample.class);
        verify(sampleRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("ABC");
        assertThat(saved.getValue().getName()).isEqualTo("Alpha");
        // Audit and identity fields must be left to JPA, never set from the request.
        assertThat(saved.getValue().getId()).isNull();
        assertThat(response.getCode()).isEqualTo("ABC");
    }

    @Test
    @DisplayName("rejects a duplicate code before touching the database")
    void rejectsDuplicateCode() {
        when(sampleRepository.existsByCodeAndDeleteFlagFalse("ABC")).thenReturn(true);
        when(messageUtil.format(eq(ErrorCode.ERR002), any(MessageKey.class))).thenReturn(LOCALISED_MESSAGE);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(LOCALISED_MESSAGE);

        verify(sampleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updates through the lock check and leaves code untouched")
    void updatesSample() {
        UUID id = UUID.randomUUID();
        OffsetDateTime token = OffsetDateTime.now();
        Sample existing = sample("ABC", "Alpha");
        when(lockSupport.loadForUpdate(sampleRepository, id, token, TemplateMessageKey.SAMPLE))
                .thenReturn(existing);

        SampleUpdateRequest request = new SampleUpdateRequest();
        request.setName("Renamed");
        request.setStatus(SampleStatus.INACTIVE);
        request.setUpdatedAt(token);

        SampleResponse response = service.update(id, request);

        assertThat(existing.getName()).isEqualTo("Renamed");
        assertThat(existing.getStatus()).isEqualTo(SampleStatus.INACTIVE);
        // The business identifier is not editable through an update.
        assertThat(existing.getCode()).isEqualTo("ABC");
        assertThat(response.getName()).isEqualTo("Renamed");
    }

    @Test
    @DisplayName("reports a missing record as not found")
    void reportsMissingRecord() {
        UUID id = UUID.randomUUID();
        when(sampleRepository.findByIdAndDeleteFlagFalse(id)).thenReturn(Optional.empty());
        when(messageUtil.format(eq(ErrorCode.ERR003), any(MessageKey.class))).thenReturn(LOCALISED_MESSAGE);

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(LOCALISED_MESSAGE);
    }

    @Test
    @DisplayName("escapes LIKE wildcards so user input is matched literally")
    void escapesSearchWildcards() {
        Page<com.luvina.base.template.projection.SampleListProjection> empty =
                new PageImpl<>(java.util.List.of(), Pageable.unpaged(), 0);
        when(sampleRepository.search(anyString(), any(), any(), any())).thenReturn(empty);

        SampleSearchRequest request = new SampleSearchRequest();
        request.setCode("100%_A");

        SamplePageResponse response = service.search(request);

        verify(sampleRepository).search(eq("100\\%\\_A"), eq(null), eq(null), any(Pageable.class));
        assertThat(response.getSampleList()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    private SampleCreateRequest createRequest() {
        SampleCreateRequest request = new SampleCreateRequest();
        request.setCode("ABC");
        request.setName("Alpha");
        request.setStatus(SampleStatus.ACTIVE);
        return request;
    }

    private Sample sample(String code, String name) {
        Sample sample = new Sample();
        sample.setCode(code);
        sample.setName(name);
        sample.setStatus(SampleStatus.ACTIVE);
        return sample;
    }
}
