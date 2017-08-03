package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static uk.gov.pay.adminusers.model.ServiceCustomisations.FIELD_BANNER_COLOUR;
import static uk.gov.pay.adminusers.model.ServiceCustomisations.FIELD_LOGO_URL;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.*;


public class ServiceRequestValidator {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceRequestValidator.class);

    public static final String FIELD_SERVICE_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";

    private final RequestValidations requestValidations;
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = new HashMap<String, List<String>>() {{
        put(FIELD_SERVICE_NAME, asList("replace"));
        put(FIELD_GATEWAY_ACCOUNT_IDS, asList("add"));
    }};

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }


    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        if (payload == null || "{}".equals(payload.toString())) {
            return Optional.empty();
        }

        if (!requestValidations.notExistOrEmptyArray().apply(payload.get(FIELD_GATEWAY_ACCOUNT_IDS))) {
            if (nonNumericGatewayAccountIds(payload.get(FIELD_GATEWAY_ACCOUNT_IDS))) {
                return Optional.of(Errors.from(format("Field [%s] must contain numeric values", FIELD_GATEWAY_ACCOUNT_IDS)));
            }
        }
        return Optional.empty();
    }

    private boolean nonNumericGatewayAccountIds(JsonNode gatewayAccountNode) {
        List<JsonNode> accountIds = Lists.newArrayList(gatewayAccountNode.elements());
        return accountIds.stream().filter(idNode -> !NumberUtils.isDigits(idNode.asText())).count() > 0;
    }

    public Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        Optional<List<String>> errors = requestValidations.checkIfExists(payload, FIELD_OP, FIELD_PATH, FIELD_VALUE);

        if (errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }
        String path = payload.get("path").asText();
        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
            return Optional.of(Errors.from(format("Path [%s] is invalid", path)));
        }

        String op = payload.get("op").asText();
        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
            return Optional.of(Errors.from(format("Operation [%s] is invalid for path [%s]", op, path)));
        }

        return Optional.empty();
    }

    public Optional<Errors> validateCustomisationRequest(JsonNode payload) {
        Optional<List<String>> errors = requestValidations.checkIfOptionalsExistsAndNotEmpty(payload, FIELD_BANNER_COLOUR, FIELD_LOGO_URL);
        if (errors.isPresent()) {
            return errors.map(Errors::from);
        }
        if (payload != null && !validLogoUrl(payload.get(FIELD_LOGO_URL))) {
            return Optional.of(Errors.from("Field [logo_url] does not comply to URI format"));
        }
        return Optional.empty();
    }

    public Optional<Errors> validateFindRequest(String gatewayAccountId) {
        if (isBlank(gatewayAccountId)) {
            return Optional.of(Errors.from("Find services currently support only by gatewayAccountId"));
        }
        if (!isNumeric(gatewayAccountId)) {
            return Optional.of(Errors.from("Query param [gatewayAccountId] must be numeric"));
        }
        return Optional.empty();
    }

    private boolean validLogoUrl(JsonNode logoUrlNode) {
        if (logoUrlNode != null) {
            String logoUrl = logoUrlNode.asText();
            try {
                new URL(logoUrl);
            } catch (MalformedURLException e) {
                LOGGER.debug("Invalid logo_url format", e);
                return false;
            }
        }
        return true;
    }
}
