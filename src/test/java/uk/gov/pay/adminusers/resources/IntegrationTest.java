package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;

public class IntegrationTest {

    static final String USERS_RESOURCE_URL = "/v1/api/users";
    static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    protected DatabaseTestHelper databaseTestHelper;
    protected ObjectMapper mapper;

    @Before
    public void setUp() {
        databaseTestHelper = app.getDatabaseTestHelper();
        mapper = new ObjectMapper();
    }

    protected RequestSpecification givenSetup() {
        return given().port(app.getLocalPort())
                .contentType(JSON);
    }
}
