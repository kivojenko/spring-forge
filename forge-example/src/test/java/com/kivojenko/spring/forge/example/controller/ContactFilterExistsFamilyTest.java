package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.ContactForgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@code @FilterFamily(matchMode = EXISTS)}: at least one of the family's targets
 * has to hold a value even when no member parameter is sent, and each supplied member narrows it further.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ContactFilterExistsFamilyTest extends WithPostgres {

  @Autowired
  private ContactForgeRepository contactRepository;

  @BeforeEach
  void setUp() throws Exception {
    contactRepository.deleteAll();

    createContact("Ada", "ada@example.com", null);
    createContact("Bob", null, "+372 5000 1111");
    createContact("Eve", "eve@example.com", "+372 5000 2222");
    createContact("Cid", null, null, "@cid");
    // Reachable by nothing at all — never matched by the family
    createContact("Dot", null, null);
  }

  @AfterEach
  void tearDown() {
    contactRepository.deleteAll();
  }

  private void createContact(String name, String email, String phone, String... handles) throws Exception {
    var body = "{"
        + "\"name\":\"" + name + "\","
        + "\"email\":" + json(email) + ","
        + "\"phone\":" + json(phone) + ","
        + "\"handles\":" + jsonArray(handles)
        + "}";

    mockMvc.perform(post("/contacts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  private static String json(String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }

  private static String jsonArray(String[] values) {
    var sb = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) sb.append(',');
      sb.append('"').append(values[i]).append('"');
    }
    return sb.append(']').toString();
  }

  @Test
  void theFamilyRequiresOneOfItsTargetsEvenWithNoParameters() throws Exception {
    mockMvc.perform(get("/contacts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)))
        .andExpect(jsonPath("$.content[?(@.name == 'Ada')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Bob')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Eve')]").exists())
        // matched through isNotEmpty() on the collection member
        .andExpect(jsonPath("$.content[?(@.name == 'Cid')]").exists())
        // neither an email, nor a phone, nor a handle
        .andExpect(jsonPath("$.content[?(@.name == 'Dot')]").doesNotExist());
  }

  @Test
  void suppliedMembersNarrowTheFamilyFurther() throws Exception {
    mockMvc.perform(get("/contacts").param("email", "example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Ada')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Eve')]").exists());

    mockMvc.perform(get("/contacts").param("handles", "cid"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Cid"));

    // Members are AND-ed with each other, so only the contact with both is left
    mockMvc.perform(get("/contacts")
            .param("email", "example.com")
            .param("phone", "5000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Eve"));
  }

  @Test
  void theFamilyIsAndEdWithFiltersOutsideIt() throws Exception {
    mockMvc.perform(get("/contacts").param("name", "ada"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Ada"));

    // The name matches, but the family still excludes an unreachable contact
    mockMvc.perform(get("/contacts").param("name", "dot"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }
}
