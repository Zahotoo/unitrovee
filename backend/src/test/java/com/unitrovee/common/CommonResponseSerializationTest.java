package com.unitrovee.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Pure serialization test: no Spring context, no database - runs fast and in CI.
 * Verifies the three envelopes serialize to the JSON
 */
class CommonResponseSerializationTest {

    // Jackson's ObjectMapper turns a Java object into JSON (and back)
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void apiResponse_hasDataAndMessage() throws Exception {
        ApiResponse<String> response = ApiResponse.ok("hello");

        // writeValueAsString = serialize to a JSON string: readTree = parse it so we can inspect fields.
        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        // expected shape: { "data": "hello", "message": "Success" }
        assertThat(json.get("data").asText()).isEqualTo("hello");
        assertThat(json.get("message").asText()).isEqualTo("Success");
    }

    @Test
    void errorReponse_hasNestedCodeAndMessage() throws Exception {
        ErrorResponse response = ErrorResponse.of("ITEM_NOT_FOUND", "The requested item does not exist");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        // expected shape: { "error": { "code": ..., "message": ... } }
        assertThat(json.get("error").get("code").asText()).isEqualTo("ITEM_NOT_FOUND");
        assertThat(json.get("error").get("message").asText()).isEqualTo("The requested item does not exist");
    }

    @Test
    void pageResponse_carriesContentAndPagingMetadata() throws Exception {
        // fake a Spring Page: items [a, b], page 0, size 20, total 2 elements
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 2);
        PageResponse<String> response = PageResponse.from(page);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        // Expected shape: { "content": [...], "page", "size", "totalElements", "totalPages" }
        assertThat(json.get("content").size()).isEqualTo(2);
        assertThat(json.get("page").asInt()).isEqualTo(0);
        assertThat(json.get("size").asInt()).isEqualTo(20);
        assertThat(json.get("totalPages").asInt()).isEqualTo(1);
        assertThat(json.get("totalElements").asLong()).isEqualTo(2);
    }
}
