package uk.gov.hmcts.ctam.jo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * 003's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class BaseLocationsFunctionalTest {

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);
}
