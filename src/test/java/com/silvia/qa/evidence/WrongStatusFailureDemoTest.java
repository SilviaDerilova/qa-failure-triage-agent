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
import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Intentional wrong-status failure to demonstrate evidence generation; enable only for a manual demo.")
class WrongStatusFailureDemoTest {
    @RegisterExtension
    final FailureEvidenceExtension evidence = new FailureEvidenceExtension();

    @Test
    @DisplayName("Intentional HTTP 201 expectation captures the actual HTTP 200 response")
    void shouldCollectEvidenceForWrongStatus() {
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .filter(evidence.filter())
                .get(PRODUCTS_LIST);

        assertThat(response.statusCode()).isEqualTo(201);
    }
}
