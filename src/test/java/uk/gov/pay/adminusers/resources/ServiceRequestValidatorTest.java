package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ServiceRequestValidatorTest {

    private static final String DEFAULT_MERCHANT_DETAILS_NAME = "Merchant name";
    private static final String DEFAULT_MERCHANT_DETAILS_EMAIL = "merchant-user@example.com";

    @Mock
    private ServiceUpdateOperationValidator mockServiceUpdateOperationValidator;

    private ServiceRequestValidator serviceRequestValidator;

    @BeforeEach
    public void setUp() {
        serviceRequestValidator = new ServiceRequestValidator(new RequestValidations(), mockServiceUpdateOperationValidator);
    }

    @Test
    public void shouldSuccess_whenValidateUpdateAttributeRequestSucceeds_withSingleOperation() {
        ObjectNode payload = createUpdateOperation("this", "will", "succeed");

        given(mockServiceUpdateOperationValidator.validate(payload)).willReturn(Collections.emptyList());

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(payload);

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldFail_whenValidateUpdateAttributeRequestFails_withSingleOperation() {
        ObjectNode payload = createUpdateOperation("this", "will", "fail");

        given(mockServiceUpdateOperationValidator.validate(payload)).willReturn(Arrays.asList("Error 1", "Error 2"));

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(payload);

        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors().size(), is(2));
        assertThat(errors.get().getErrors(), hasItem("Error 1"));
        assertThat(errors.get().getErrors(), hasItem("Error 2"));
    }

    @Test
    public void shouldSuccess_whenValidateUpdateAttributeRequestSucceeds_withArrayOfOperations() {
        ObjectNode operation1 = createUpdateOperation("the", "first", "operation");
        ObjectNode operation2 = createUpdateOperation("the", "second", "operation");

        ArrayNode payload = JsonNodeFactory.instance.arrayNode().add(operation1).add(operation2);

        given(mockServiceUpdateOperationValidator.validate(operation1)).willReturn(Collections.emptyList());
        given(mockServiceUpdateOperationValidator.validate(operation2)).willReturn(Collections.emptyList());

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(payload);

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldFail_whenValidateUpdateAttributeRequestFails_withArrayOfOperations() {
        ObjectNode operation1 = createUpdateOperation("the", "first", "operation");
        ObjectNode operation2 = createUpdateOperation("the", "second", "operation");
        ObjectNode operation3 = createUpdateOperation("the", "third", "operation");

        ArrayNode payload = JsonNodeFactory.instance.arrayNode().add(operation1).add(operation2).add(operation3);

        given(mockServiceUpdateOperationValidator.validate(operation1)).willReturn(Collections.emptyList());
        given(mockServiceUpdateOperationValidator.validate(operation2)).willReturn(Arrays.asList("Error 1", "Error 2"));
        given(mockServiceUpdateOperationValidator.validate(operation3)).willReturn(Collections.singletonList("Error 3"));

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(payload);

        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors().size(), is(3));
        assertThat(errors.get().getErrors(), hasItem("Error 1"));
        assertThat(errors.get().getErrors(), hasItem("Error 2"));
        assertThat(errors.get().getErrors(), hasItem("Error 3"));
    }

    @Test
    public void shouldAllowNonNumericGatewayAccounts_whenFindingServices() {
        Optional<Errors> errors = serviceRequestValidator.validateFindRequest("non-numeric-id");
        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldSuccess_updatingMerchantDetails() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, DEFAULT_MERCHANT_DETAILS_NAME);
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_EMAIL, DEFAULT_MERCHANT_DETAILS_EMAIL);

        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test
    public void shouldFail_updatingMerchantDetails_forEmptyObject() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(ValidationException.class, () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_forMissingMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");

        assertThrows(ValidationException.class, () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_forBlankStringMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, "");

        assertThrows(ValidationException.class, () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_forNullValueMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, null);

        assertThrows(ValidationException.class, () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenInvalidEmail() throws ValidationException {
        ObjectNode payload = createMerchantDetailsJsonPayload(DEFAULT_MERCHANT_DETAILS_NAME, "invalid@example.com-uk");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));

        assertThat(validationException.getErrors().getErrors(), hasItem("Field [email] must be a valid email address"));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenEmailOver255() throws ValidationException {
        String longEmail = randomAlphanumeric(256);
        ObjectNode payload = createMerchantDetailsJsonPayload(DEFAULT_MERCHANT_DETAILS_NAME, longEmail);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));

        assertThat(validationException.getErrors().getErrors(), hasItem("Field [email] must have a maximum length of 255 characters"));
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenNameOver255() throws ValidationException {
        String longName = randomAlphanumeric(256);
        ObjectNode payload = createMerchantDetailsJsonPayload(longName, DEFAULT_MERCHANT_DETAILS_EMAIL);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload));

        assertThat(validationException.getErrors().getErrors(), hasItem("Field [name] must have a maximum length of 255 characters"));
    }

    private static ObjectNode createMerchantDetailsJsonPayload(String name, String email) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, name);
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_EMAIL, email);
        return payload;
    }

    private static ObjectNode createUpdateOperation(String path, String op, String value) {
        ObjectNode operation = JsonNodeFactory.instance.objectNode();
        operation.put(ServiceUpdateRequest.FIELD_PATH, path);
        operation.put(ServiceUpdateRequest.FIELD_OP, op);
        operation.put(ServiceUpdateRequest.FIELD_VALUE, value);
        return operation;
    }
}
