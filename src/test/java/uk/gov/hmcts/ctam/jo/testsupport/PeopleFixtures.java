package uk.gov.hmcts.ctam.jo.testsupport;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.mappers.PeopleMapper;
import uk.gov.hmcts.ctam.jo.mappers.PeopleMapperImpl;
import uk.gov.hmcts.ctam.jo.repository.FixturePeopleRepository;
import uk.gov.hmcts.ctam.jo.repository.FixtureReferenceDataRepository;
import uk.gov.hmcts.ctam.jo.repository.ReferenceDataRepository;
import uk.gov.hmcts.ctam.jo.services.AppointmentStatus;
import uk.gov.hmcts.ctam.jo.services.AuthorisationSummaries;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * People test objects for the unit suite, built from the shipped configuration ({@code application.yml}) and the
 * shipped reference data, so the People tests resolve names exactly as the service does. Everything is loaded once.
 */
public final class PeopleFixtures {

    /**
     * The shipped default reference date (013 FR-020).
     */
    public static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-01-01");

    public static final String MINIMAL = "classpath:people/minimal.json";

    public static final String DEFAULT = "classpath:people/office_holders.json";

    /**
     * The first office holder of minimal.json: current and previous appointments, every optional field set.
     */
    public static final String FIRST_ID = "11111111-1111-4111-8111-111111111111";

    /**
     * The second office holder of minimal.json: one current appointment, every optional field unset.
     */
    public static final String SECOND_ID = "22222222-2222-4222-8222-222222222222";

    private static final Path MAIN_CONFIGURATION = Path.of("src/main/resources/application.yml");

    private static Binder binder;

    private static ReferenceDataTypeRegistry registry;

    private static ReferenceDataRepository referenceData;

    private PeopleFixtures() {
    }

    /**
     * The shipped {@code jo.people} settings, with another fixture.
     */
    public static PeopleProperties properties(String fixture) {
        PeopleProperties shipped = binder().bind("jo.people", PeopleProperties.class)
                .orElseThrow(() -> new AssertionError("jo.people is not configured"));
        return new PeopleProperties(fixture, shipped.referenceDate(), shipped.referenceTypes());
    }

    public static synchronized ReferenceDataTypeRegistry registry() {
        if (registry == null) {
            registry = new ReferenceDataTypeRegistry(binder().bind("jo.reference-data", ReferenceDataProperties.class)
                                                             .get());
        }
        return registry;
    }

    public static synchronized ReferenceDataRepository referenceData() {
        if (referenceData == null) {
            referenceData = new FixtureReferenceDataRepository(registry(), new DefaultResourceLoader(),
                                                               JsonMapper.builder().build());
        }
        return referenceData;
    }

    public static AppointmentStatus appointmentStatus() {
        return new AppointmentStatus(properties(MINIMAL));
    }

    /**
     * Loads a People fixture against the shipped reference data.
     */
    public static FixturePeopleRepository repository(String fixture) {
        return repository(properties(fixture), registry());
    }

    public static FixturePeopleRepository repository(PeopleProperties properties, ReferenceDataTypeRegistry types) {
        return new FixturePeopleRepository(properties, types, referenceData(), new AppointmentStatus(properties),
                                           new DefaultResourceLoader(), JsonMapper.builder().build());
    }

    /**
     * The generated mapper, wired as Spring wires it, with the shipped reference date.
     */
    public static PeopleMapper mapper() {
        PeopleMapperImpl mapper = new PeopleMapperImpl();
        mapper.setAppointmentStatus(appointmentStatus());
        mapper.setAuthorisationSummaries(new AuthorisationSummaries());
        return mapper;
    }

    private static synchronized Binder binder() {
        if (binder == null) {
            try {
                List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                        .load("application.yml", new FileSystemResource(MAIN_CONFIGURATION));
                // Resolves ${JO_PEOPLE_REFERENCE_DATE:2026-01-01} and the like to their defaults.
                binder = new Binder(ConfigurationPropertySources.from(sources),
                                    new PropertySourcesPlaceholdersResolver(sources));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return binder;
    }
}
