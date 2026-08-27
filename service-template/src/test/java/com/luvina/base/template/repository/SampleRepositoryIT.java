package com.luvina.base.template.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.luvina.base.template.entity.Sample;
import com.luvina.base.template.enums.SampleStatus;
import com.luvina.base.template.projection.SampleListProjection;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests against a real PostgreSQL instance.
 *
 * <p>Named {@code *IT}, so {@code mvn test} skips it and a machine without
 * Docker can still build. Run it with {@code mvn verify -Pit}.
 *
 * <p>A real database is not optional for this layer. The native search relies on
 * PostgreSQL casts and on a partial unique index; an in-memory database would
 * either reject the SQL or accept writes that production rejects. The schema is
 * created by the same Flyway migrations that run in production, so the migration
 * itself is under test too.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("SampleRepository")
class SampleRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("hides soft-deleted rows from the base lookups")
    void hidesSoftDeletedRows() {
        Sample visible = persist("KEEP", "Kept");
        Sample deleted = persist("GONE", "Removed");
        deleted.setDeleteFlag(true);
        flush();

        Optional<Sample> foundVisible = sampleRepository.findByIdAndDeleteFlagFalse(visible.getId());
        Optional<Sample> foundDeleted = sampleRepository.findByIdAndDeleteFlagFalse(deleted.getId());

        assertThat(foundVisible).isPresent();
        assertThat(foundDeleted).isEmpty();
        assertThat(sampleRepository.existsByCodeAndDeleteFlagFalse("GONE")).isFalse();
    }

    @Test
    @DisplayName("fills the audit columns automatically")
    void fillsAuditColumns() {
        Sample sample = persist("AUDIT", "Audited");
        flush();

        assertThat(sample.getId()).isNotNull();
        assertThat(sample.getCreatedAt()).isNotNull();
        assertThat(sample.getUpdatedAt()).isNotNull();
        assertThat(sample.getDeleteFlag()).isFalse();
    }

    @Test
    @DisplayName("filters and pages the native search")
    void filtersNativeSearch() {
        persist("AAA", "Alpha");
        persist("BBB", "Beta");
        Sample inactive = persist("CCC", "Gamma");
        inactive.setStatus(SampleStatus.INACTIVE);
        flush();

        Page<SampleListProjection> byName = sampleRepository.search(
                null, "alp", null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "code")));
        assertThat(byName.getTotalElements()).isEqualTo(1);
        assertThat(byName.getContent().get(0).getCode()).isEqualTo("AAA");

        Page<SampleListProjection> active = sampleRepository.search(
                null, null, SampleStatus.ACTIVE.name(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "code")));
        assertThat(active.getContent()).extracting(SampleListProjection::getCode)
                .containsExactly("AAA", "BBB");

        Page<SampleListProjection> firstPage = sampleRepository.search(
                null, null, null, PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "code")));
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("treats a LIKE wildcard in the filter as a literal")
    void treatsWildcardAsLiteral() {
        persist("A100", "Plain");
        persist("A%00", "Literal");
        flush();

        // The escaped pattern must match only the row that really contains "%".
        Page<SampleListProjection> escaped = sampleRepository.search(
                "a\\%00", null, null, PageRequest.of(0, 10, Sort.by("code")));

        assertThat(escaped.getContent()).extracting(SampleListProjection::getCode)
                .containsExactly("A%00");
    }

    @Test
    @DisplayName("lets a soft-deleted code be reused")
    void allowsReuseOfDeletedCode() {
        Sample first = persist("REUSE", "First");
        flush();
        first.setDeleteFlag(true);
        flush();

        // The unique index is partial, so the live row is the only one that
        // counts. A plain UNIQUE constraint would reject this.
        persist("REUSE", "Second");
        flush();

        List<Sample> all = sampleRepository.findAllByDeleteFlagFalse(Sort.by("code"));
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getName()).isEqualTo("Second");
    }

    private Sample persist(String code, String name) {
        Sample sample = new Sample();
        sample.setCode(code);
        sample.setName(name);
        sample.setStatus(SampleStatus.ACTIVE);
        sample.setEffectiveDate(LocalDate.of(2026, 1, 1));
        return sampleRepository.save(sample);
    }

    /**
     * Pushes pending changes to the database so that the next native query sees
     * them. The context is deliberately not cleared: entities stay managed, and
     * an intermediate flush is what forces an UPDATE to reach the database
     * before a later INSERT that would otherwise collide with it.
     */
    private void flush() {
        entityManager.flush();
    }
}
