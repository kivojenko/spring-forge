package com.kivojenko.spring.forge.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.slashes.Document;
import com.kivojenko.spring.forge.example.model.slashes.DocumentDefault;
import com.kivojenko.spring.forge.example.model.slashes.DocumentDefaultForgeController;
import com.kivojenko.spring.forge.example.model.slashes.DocumentForgeController;
import com.kivojenko.spring.forge.example.model.slashes.DocumentNoSlashes;
import com.kivojenko.spring.forge.example.model.slashes.DocumentNoSlashesForgeController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class DocumentControllerTest extends WithPostgres {

  @AfterEach
  void cleanUpDocuments() throws Exception {
    String docsJson = mockMvc
        .perform(get("/documents"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode docsRoot = objectMapper.readTree(docsJson);
    for (JsonNode doc : docsRoot.get("content")) {
      String id = doc.get("id").asText();
      mockMvc.perform(delete("/documents/" + id)).andExpect(status().isNoContent());
    }
  }

  @Test
  void testCrudWithSlashesInId() throws Exception {
    String docId = "/docs/2026/report/q1.pdf";
    Document doc = Document.builder()
        .id(docId)
        .title("Q1 Report")
        .content("Initial Content")
        .build();

    // 1. Create document
    mockMvc.perform(post("/documents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(doc)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(docId)))
        .andExpect(jsonPath("$.title", is("Q1 Report")));

    // 2. Get document by ID containing slashes (GET /documents/{*id})
    mockMvc.perform(get("/documents" + docId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(docId)))
        .andExpect(jsonPath("$.title", is("Q1 Report")))
        .andExpect(jsonPath("$.content", is("Initial Content")));

    // 3. Exists check (HEAD /documents/{*id})
    mockMvc.perform(head("/documents" + docId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", is(true)));

    // 4. Update document (PUT /documents/{*id})
    Document updated = Document.builder()
        .id(docId)
        .title("Updated Q1 Report")
        .content("Updated Content")
        .build();

    mockMvc.perform(put("/documents" + docId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updated)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(docId)))
        .andExpect(jsonPath("$.title", is("Updated Q1 Report")))
        .andExpect(jsonPath("$.content", is("Updated Content")));

    // 5. Patch document (PATCH /documents/{*id})
    mockMvc.perform(patch("/documents" + docId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("title", "Patched Q1 Report"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(docId)))
        .andExpect(jsonPath("$.title", is("Patched Q1 Report")))
        .andExpect(jsonPath("$.content", is("Updated Content")));

    // 6. Delete document (DELETE /documents/{*id})
    mockMvc.perform(delete("/documents" + docId))
        .andExpect(status().isNoContent());

    // 7. Verify deletion
    mockMvc.perform(head("/documents" + docId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", is(false)));
  }

  @Test
  void testMappingPlaceholders() throws Exception {
    // Document has @WithRestController(allowSlashes = true) -> "{*id}"
    assertEquals("/{*id}", DocumentForgeController.class.getMethod("getById", String.class).getAnnotation(GetMapping.class).value()[0]);
    assertEquals("/{*id}", DocumentForgeController.class.getMethod("update", String.class, Document.class).getAnnotation(PutMapping.class).value()[0]);
    assertEquals("/{*id}", DocumentForgeController.class.getMethod("patch", String.class, Map.class).getAnnotation(PatchMapping.class).value()[0]);
    assertEquals("/{*id}", DocumentForgeController.class.getMethod("delete", String.class).getAnnotation(DeleteMapping.class).value()[0]);

    // DocumentNoSlashes has @WithRestController(allowSlashes = false) -> "{id}"
    assertEquals("/{id}", DocumentNoSlashesForgeController.class.getMethod("getById", String.class).getAnnotation(GetMapping.class).value()[0]);
    assertEquals("/{id}", DocumentNoSlashesForgeController.class.getMethod("update", String.class, DocumentNoSlashes.class).getAnnotation(PutMapping.class).value()[0]);
    assertEquals("/{id}", DocumentNoSlashesForgeController.class.getMethod("patch", String.class, Map.class).getAnnotation(PatchMapping.class).value()[0]);
    assertEquals("/{id}", DocumentNoSlashesForgeController.class.getMethod("delete", String.class).getAnnotation(DeleteMapping.class).value()[0]);

    // DocumentDefault has @WithRestController without explicit allowSlashes -> "{id}" (from default config)
    assertEquals("/{id}", DocumentDefaultForgeController.class.getMethod("getById", String.class).getAnnotation(GetMapping.class).value()[0]);
    assertEquals("/{id}", DocumentDefaultForgeController.class.getMethod("update", String.class, DocumentDefault.class).getAnnotation(PutMapping.class).value()[0]);
    assertEquals("/{id}", DocumentDefaultForgeController.class.getMethod("patch", String.class, Map.class).getAnnotation(PatchMapping.class).value()[0]);
    assertEquals("/{id}", DocumentDefaultForgeController.class.getMethod("delete", String.class).getAnnotation(DeleteMapping.class).value()[0]);
  }
}
