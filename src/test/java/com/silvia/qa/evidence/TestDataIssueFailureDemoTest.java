package com.silvia.qa.evidence;

import com.silvia.qa.support.evidence.FailureEvidenceExtension;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.silvia.qa.api.Endpoints.SEARCH_PRODUCT;
import static com.silvia.qa.config.Config.BASE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Disabled("Intentional TEST_DATA_ISSUE failure; enable only for a manual evidence demo.")
class TestDataIssueFailureDemoTest {
    @RegisterExtension
    final FailureEvidenceExtension evidence = new FailureEvidenceExtension();

    @Test
    @DisplayName("Missing search_product test data causes a controlled failure")
    void shouldCollectEvidenceForMissingSearchTestData() {
        // Intentionally incomplete fixture: omit the required search_product form parameter.
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .filter(evidence.filter())
                .post(SEARCH_PRODUCT);

        // Confirm the API correctly rejects the bad input before triggering the demo failure.
        assertThat(response.jsonPath().getInt("responseCode")).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
                .containsIgnoringCase("search_product parameter is missing");

        fail("Required test data 'search_product' is missing; scenario cannot continue.");
    }
}
