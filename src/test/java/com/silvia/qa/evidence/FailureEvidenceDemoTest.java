package com.silvia.qa.evidence;

import com.silvia.qa.support.evidence.FailureEvidenceExtension;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.silvia.qa.api.Endpoints.PRODUCTS_LIST;
import static com.silvia.qa.config.Config.BASE_URL;
import static org.assertj.core.api.Assertions.fail;

@Disabled("Intentional failure to demonstrate evidence generation; temporarily remove @Disabled and run this class explicitly.")
class FailureEvidenceDemoTest {
    @RegisterExtension
    final FailureEvidenceExtension evidence = new FailureEvidenceExtension();

    @Test
    @DisplayName("Intentional API failure generates a JSON evidence file")
    void shouldCollectEvidenceForIntentionalFailure() {
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .filter(evidence.filter())
                .get(PRODUCTS_LIST);

        fail("Intentional demo failure after API response with status " + response.statusCode());
    }
}
