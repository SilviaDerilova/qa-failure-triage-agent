package com.silvia.qa.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.silvia.qa.api.Endpoints.PRODUCTS_LIST;
import static com.silvia.qa.config.Config.BASE_URL;
import static org.assertj.core.api.Assertions.assertThat;

class ProductsUnsupportedMethodApiTest {

    @Test
    @DisplayName("POST /api/productsList rejects an unsupported request method")
    void shouldRejectPostToProductsList() {
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .post(PRODUCTS_LIST);

        // This API reports the business error in the JSON payload.
        assertThat(response.jsonPath().getInt("responseCode")).isEqualTo(405);
        assertThat(response.jsonPath().getString("message"))
                .containsIgnoringCase("request method is not supported");
    }
}
