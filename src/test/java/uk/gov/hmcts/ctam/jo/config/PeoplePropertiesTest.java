package uk.gov.hmcts.ctam.jo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.properties;

/**
 * {@code jo.people} binds the shipped defaults and requires every setting (013 research R4, data-model.md § 6).
 */
class PeoplePropertiesTest {

    private static final List<String> COMPLETE = List.of(
            "jo.people.fixture=classpath:people/minimal.json",
            "jo.people.reference-date=2026-01-01",
            "jo.people.reference-types.gender-id=a",
            "jo.people.reference-types.role-name-id=b",
            "jo.people.reference-types.contract-type-id=c",
            "jo.people.reference-types.base-location-id=d",
            "jo.people.reference-types.base-location-type-id=e",
            "jo.people.reference-types.base-location-parent-id=f",
            "jo.people.reference-types.jurisdiction-id=g",
            "jo.people.reference-types.judiciary-role-name-id=h",
            "jo.people.reference-types.ticket-id=i",
            "jo.people.reference-types.ticket-category-id=j");

    @Configuration
    @EnableConfigurationProperties(PeopleProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void theShippedDefaultsBind() {
        PeopleProperties shipped = properties("classpath:people/office_holders.json");

        assertThat(shipped.referenceDate()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(shipped.referenceTypes().genderId()).isEqualTo("genders");
        assertThat(shipped.referenceTypes().ticketCategoryId()).isEqualTo("ticket_categories");
    }

    @Test
    void completeConfigurationBinds() {
        runner.withPropertyValues(COMPLETE.toArray(String[]::new)).run(context -> {
            assertThat(context).hasNotFailed();
            PeopleProperties bound = context.getBean(PeopleProperties.class);
            assertThat(bound.referenceDate()).isEqualTo(LocalDate.parse("2026-01-01"));
            assertThat(bound.referenceTypes().baseLocationParentId()).isEqualTo("f");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"jo.people.fixture", "jo.people.reference-date", "jo.people.reference-types.gender-id",
        "jo.people.reference-types.ticket-category-id"})
    void missingSettingFailsValidation(String key) {
        List<String> without = new ArrayList<>(COMPLETE.stream().filter(line -> !line.startsWith(key + "=")).toList());

        runner.withPropertyValues(without.toArray(String[]::new)).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankReferenceTypeFailsValidation() {
        List<String> blank = new ArrayList<>(COMPLETE);
        blank.add("jo.people.reference-types.ticket-id= ");

        runner.withPropertyValues(blank.toArray(String[]::new)).run(context -> assertThat(context).hasFailed());
    }
}
