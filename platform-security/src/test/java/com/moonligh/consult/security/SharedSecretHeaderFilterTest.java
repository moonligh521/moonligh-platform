package com.moonligh.consult.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class SharedSecretHeaderFilterTest {

    @Test
    void rejectsMissingTokenOnPrefixedPath() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Probe())
                .addFilters(new SharedSecretHeaderFilter(
                        "X-Pay-Token", "secret", "/api/payments", SharedSecretHeaderFilter.FailBody.SIMPLE_UNAUTHORIZED))
                .build();
        mockMvc.perform(get("/api/payments/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("{\"code\":\"UNAUTHORIZED\"}"));
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Probe())
                .addFilters(new SharedSecretHeaderFilter(
                        "X-Pay-Token", "secret", "/api/payments", SharedSecretHeaderFilter.FailBody.SIMPLE_UNAUTHORIZED))
                .build();
        String body = mockMvc.perform(get("/api/payments/orders").header("X-Pay-Token", "secret"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals("ok", body);
    }

    @RestController
    static class Probe {
        @GetMapping("/api/payments/orders")
        String orders() {
            return "ok";
        }
    }
}
