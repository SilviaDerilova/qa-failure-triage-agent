package com.silvia.qa.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.silvia.qa.api.Endpoints.PRODUCTS_LIST;
import static com.silvia.qa.config.Config.BASE_URL;
import static org.assertj.core.api.Assertions.assertThat;

class ProductsApiTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
    }

    @Test
    @DisplayName("GET /api/productsList returns a non-empty products list")
    void shouldReturnAllProducts() {
        // Given / When
        Response response = RestAssured
                .given()
                .when()
                .get(PRODUCTS_LIST)
                .then()
                .extract()
                .response();

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("responseCode")).isEqualTo(200);
        assertThat(response.jsonPath().getList("products")).isNotEmpty();
    }
}
