package com.silvia.qa.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.silvia.qa.api.Endpoints.SEARCH_PRODUCT;
import static com.silvia.qa.config.Config.BASE_URL;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchApiTest {

    @Test
    @DisplayName("POST /api/searchProduct returns products matching top")
    void shouldReturnProductsMatchingSearchTerm() {
        String searchTerm = "top";
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .formParam("search_product", searchTerm)
                .post(SEARCH_PRODUCT);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("responseCode")).isEqualTo(200);
        List<?> products = response.jsonPath().getList("products");
        assertThat(products).isNotEmpty();
        // Search matches product names or categories, such as "Tops & Shirts".
        for (int index = 0; index < products.size(); index++) {
            String name = response.jsonPath().getString("products[" + index + "].name");
            String category = response.jsonPath().getString("products[" + index + "].category.category");
            assertThat(name).isNotBlank();
            assertThat(category).isNotBlank();
            assertThat(name + " " + category)
                    .as("Search relevance of product '%s' in category '%s'", name, category)
                    .containsIgnoringCase(searchTerm);
        }
    }

    @Test
    @DisplayName("POST /api/searchProduct rejects a missing search_product parameter")
    void shouldRejectSearchWithoutSearchProductParameter() {
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .post(SEARCH_PRODUCT);

        assertThat(response.jsonPath().getInt("responseCode")).isEqualTo(400);
        assertThat(response.jsonPath().getString("message"))
                .containsIgnoringCase("search_product parameter is missing");
    }
}
