package com.example.controller;

import com.example.api.WarmestDataStructureInterface;
import com.example.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

    private static final String BASE_PATH = "/api/v1/warmest";

    @Autowired
    private MockMvc mockMvc; //simulates HTTP requests without starting a real server like Tomcat on port 8080

    @MockitoBean
    private WarmestDataStructureInterface service;

    @Test
    void putReturnsPreviousValue() throws Exception {
        when(service.put("a", 101)).thenReturn(100);

        mockMvc.perform(put(BASE_PATH + "/a")
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
        mockMvc.perform(put(BASE_PATH + "/a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.detail").value("value: value is required"))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/a"));
    }

    @Test
    void getReturnsValueWhenKeyExists() throws Exception {
        when(service.get("a")).thenReturn(100);

        mockMvc.perform(get(BASE_PATH + "/a"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void getReturnsNotFoundWhenKeyDoesNotExist() throws Exception {
        when(service.get("missing")).thenReturn(null);

        mockMvc.perform(get(BASE_PATH + "/missing"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deleteReturnsPreviousValueWhenKeyExists() throws Exception {
        when(service.remove("a")).thenReturn(100);

        mockMvc.perform(delete(BASE_PATH + "/a"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void deleteReturnsNotFoundWhenKeyDoesNotExist() throws Exception {
        when(service.remove("missing")).thenReturn(null);

        mockMvc.perform(delete(BASE_PATH + "/missing"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void getWarmestReturnsWarmestKey() throws Exception {
        when(service.getWarmest()).thenReturn("a");

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string("a"));
    }

    @Test
    void getWarmestReturnsNotFoundWhenStructureIsEmpty() throws Exception {
        when(service.getWarmest()).thenReturn(null);

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Warmest not found"))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.detail").value("Warmest not found"))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }
}
