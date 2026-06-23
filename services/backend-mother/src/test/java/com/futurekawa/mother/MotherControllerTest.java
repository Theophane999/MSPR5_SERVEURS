package com.futurekawa.mother;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MotherController.class)
class MotherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AggregationService aggregationService;

    @Test
    void shouldReturnAggregatedChildren() throws Exception {
        var response = new AggregationService.AggregatedResponse(
            "mother",
            "2026-01-01T00:00:00Z",
            List.of(new AggregationService.ChildAggregate(
                "brazil",
                "http://backend-brazil:3000",
                true,
                Map.of("country", "Brazil", "message", "ok"),
                null,
                null
            ))
        );

        when(aggregationService.aggregate()).thenReturn(response);

        mockMvc.perform(get("/api/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("mother"))
            .andExpect(jsonPath("$.children[0].data.country").value("Brazil"));
    }
}
