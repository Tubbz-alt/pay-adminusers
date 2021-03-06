package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TelephoneNumberUtilityFormatToE164ValidDataTest {

    @Test
    public void formatToE164_shouldEvaluateToE164FormattedAmericanTelephoneNumber() {
        // Given
        String telephoneNumber = "+13115552368";
        String testResult = "+13115552368";

        // When
        String result = TelephoneNumberUtility.formatToE164(telephoneNumber);

        // Then
        assertThat(result, is(testResult));
    }

}
