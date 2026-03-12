package com.example.controller;

import com.example.api.WarmestDataStructureInterface;
import com.example.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarmestDataStructureController.class)
@Import(GlobalExceptionHandler.class)
class WarmestDataStructureControllerTest {

    @Autowired
    private MockMvc mockMvc; //simulates HTTP requests without starting a real server like Tomcat on port 8080

    @MockitoBean
    private WarmestDataStructureInterface service;

    @Test
    void putReturnsPreviousValue() throws Exception {
        when(service.put("a", 101)).thenReturn(100);

        mockMvc.perform(put("/api/v1/warmest/a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": 101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void putReturnsBadRequestWhenValueIsMissing() throws Exception {
        mockMvc.perform(put("/api/v1/warmest/a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("value: value is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/warmest/a"));
    }

    @Test
    void getReturnsValueWhenKeyExists() throws Exception {
        when(service.get("a")).thenReturn(100);

        mockMvc.perform(get("/api/v1/warmest/a"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void getReturnsNotFoundWhenKeyDoesNotExist() throws Exception {
        when(service.get("missing")).thenReturn(null);

        mockMvc.perform(get("/api/v1/warmest/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Key not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Key not found: missing"))
                .andExpect(jsonPath("$.path").value("/api/v1/warmest/missing"));
    }

    @Test
    void deleteReturnsPreviousValueWhenKeyExists() throws Exception {
        when(service.remove("a")).thenReturn(100);

        mockMvc.perform(delete("/api/v1/warmest/a"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void deleteReturnsNotFoundWhenKeyDoesNotExist() throws Exception {
        when(service.remove("missing")).thenReturn(null);

        mockMvc.perform(delete("/api/v1/warmest/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Key not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Key not found: missing"))
                .andExpect(jsonPath("$.path").value("/api/v1/warmest/missing"));
    }

    @Test
    void getWarmestReturnsWarmestKey() throws Exception {
        when(service.getWarmest()).thenReturn("a");

        mockMvc.perform(get("/api/v1/warmest"))
                .andExpect(status().isOk())
                .andExpect(content().string("a"));
    }

    @Test
    void getWarmestReturnsNotFoundWhenStructureIsEmpty() throws Exception {
        when(service.getWarmest()).thenReturn(null);

        mockMvc.perform(get("/api/v1/warmest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Warmest not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Warmest not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/warmest"));
    }
}
