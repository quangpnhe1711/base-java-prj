package com.luvina.base.template.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luvina.base.core.config.MessageSourceConfig;
import com.luvina.base.core.exception.GlobalExceptionHandler;
import com.luvina.base.core.exception.ResourceNotFoundException;
import com.luvina.base.core.i18n.MessageUtil;
import com.luvina.base.template.dto.request.SampleCreateRequest;
import com.luvina.base.template.dto.response.SampleResponse;
import com.luvina.base.template.enums.SampleStatus;
import com.luvina.base.template.service.SampleService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests: routing, request binding, validation and the shape of the
 * error body. The service is mocked, so nothing here touches a database.
 *
 * <p>{@code @WebMvcTest} does not load auto-configurations from the shared
 * modules, which keeps the test offline: the real security setup would try to
 * fetch the identity provider metadata. The error handler is imported
 * explicitly, because the error contract is exactly what these tests assert.
 *
 * <p>Security filters are switched off with {@code addFilters = false}. Left on,
 * every request would be answered by the framework default chain, which is not
 * the chain this service actually runs, so the assertions would describe
 * something that does not exist in production. Authentication and authorisation
 * belong in an integration test that loads the real filter chain.
 */
@WebMvcTest(controllers = SampleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MessageSourceConfig.class, MessageUtil.class})
@DisplayName("SampleController")
class SampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SampleService sampleService;

    @Test
    @DisplayName("returns a sample as JSON")
    void returnsSample() throws Exception {
        UUID id = UUID.randomUUID();
        when(sampleService.getById(id)).thenReturn(response(id));

        mockMvc.perform(get("/api/v1/samples/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC"))
                .andExpect(jsonPath("$.name").value("Alpha"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // The lock token must be part of every read, or the client
                // cannot perform a safe update afterwards.
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("answers 201 with a Location header on create")
    void createsSample() throws Exception {
        UUID id = UUID.randomUUID();
        when(sampleService.create(any(SampleCreateRequest.class))).thenReturn(response(id));

        SampleCreateRequest request = new SampleCreateRequest();
        request.setCode("ABC");
        request.setName("Alpha");
        request.setStatus(SampleStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/samples/" + id));
    }

    @Test
    @DisplayName("reports invalid input as ERR014 with per-field detail")
    void reportsValidationErrors() throws Exception {
        SampleCreateRequest request = new SampleCreateRequest();
        request.setCode("lower case");
        request.setName("");

        mockMvc.perform(post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR014"))
                .andExpect(jsonPath("$.path").value("/api/v1/samples"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'code')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    @DisplayName("maps a missing record to 404 in the standard error shape")
    void reportsMissingRecord() throws Exception {
        UUID id = UUID.randomUUID();
        when(sampleService.getById(id)).thenThrow(new ResourceNotFoundException("Sample does not exist."));

        mockMvc.perform(get("/api/v1/samples/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR003"))
                .andExpect(jsonPath("$.message").value("Sample does not exist."));
    }

    @Test
    @DisplayName("rejects a malformed id before reaching the service")
    void rejectsMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/samples/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR017"));
    }

    private SampleResponse response(UUID id) {
        SampleResponse response = new SampleResponse();
        response.setId(id);
        response.setCode("ABC");
        response.setName("Alpha");
        response.setStatus(SampleStatus.ACTIVE);
        response.setEffectiveDate(LocalDate.of(2026, 1, 1));
        response.setUpdatedAt(OffsetDateTime.now());
        return response;
    }
}
