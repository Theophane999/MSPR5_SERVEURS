package com.futurekawa.child;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InfoController.class)
@TestPropertySource(properties = {"app.country=Brazil", "app.mqtt.enabled=false"})
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnChildInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("child"))
            .andExpect(jsonPath("$.country").value("Brazil"));
    }
}
