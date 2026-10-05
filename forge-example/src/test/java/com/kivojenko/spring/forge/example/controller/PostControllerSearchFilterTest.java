package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Post;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class PostControllerSearchFilterTest extends WithPostgres {

  @BeforeEach
  void setUp() throws Exception {
    createPost(Post.builder()
        .title("Getting Started with Spring Boot")
        .content("Learn how to build modern web applications.")
        .slug("spring-boot-guide")
        .tags(List.of("java", "spring", "backend"))
        .published(true)
        .authorName("John Doe")
        .build());

    createPost(Post.builder()
        .title("Advanced QueryDSL Patterns")
        .content("Build complex dynamic SQL predicates easily.")
        .slug("querydsl-patterns")
        .tags(List.of("sql", "jpa"))
        .published(true)
        .authorName("Jane Smith")
        .build());

    createPost(Post.builder()
        .title("Draft Architecture Notes")
        .content("Internal thoughts on microservices and spring.")
        .slug("draft-arch")
        .tags(List.of("architecture"))
        .published(false)
        .authorName("John Doe")
        .build());
  }

  @AfterEach
  void cleanUpPosts() throws Exception {
    var ids = jdbcTemplate.queryForList("SELECT id FROM posts", Long.class);
    for (Long id : ids) {
      mockMvc.perform(delete("/posts/{id}", id)).andExpect(status().isNoContent());
    }
  }

  private void createPost(Post post) throws Exception {
    mockMvc.perform(post("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(post)))
        .andExpect(status().isCreated());
  }

  @Test
  void testGeneratedFilterFields() throws Exception {
    Class<?> filterClass = Class.forName("com.kivojenko.spring.forge.example.model.filter.PostForgeFilter");
    Field[] fields = filterClass.getDeclaredFields();

    Field searchField = Arrays.stream(fields)
        .filter(f -> f.getName().equals("search"))
        .findFirst()
        .orElse(null);
    assertNotNull(searchField, "Expected 'search' field in PostForgeFilter");
    assertEquals(String.class, searchField.getType());

    Field queryField = Arrays.stream(fields)
        .filter(f -> f.getName().equals("query"))
        .findFirst()
        .orElse(null);
    assertNotNull(queryField, "Expected 'query' field in PostForgeFilter");
    assertEquals(String.class, queryField.getType());

    Field publishedField = Arrays.stream(fields)
        .filter(f -> f.getName().equals("published"))
        .findFirst()
        .orElse(null);
    assertNotNull(publishedField, "Expected 'published' field in PostForgeFilter");
    assertEquals(Boolean.class, publishedField.getType());
  }

  @Test
  void testSearchMatchingTitle() throws Exception {
    // "getting" matches only the first post's title
    mockMvc.perform(get("/posts").param("search", "getting"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title", is("Getting Started with Spring Boot")));
  }

  @Test
  void testSearchMatchingContent() throws Exception {
    // "predicates" matches only the second post's content
    mockMvc.perform(get("/posts").param("search", "predicates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title", is("Advanced QueryDSL Patterns")));
  }

  @Test
  void testSearchMatchingSlugPrefix() throws Exception {
    // "spring-" matches slug starting with "spring-"
    mockMvc.perform(get("/posts").param("search", "spring-"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].slug", is("spring-boot-guide")));
  }

  @Test
  void testSearchMatchingTagsCollection() throws Exception {
    // "backend" matches tag in post 1
    mockMvc.perform(get("/posts").param("search", "backend"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title", is("Getting Started with Spring Boot")));
  }

  @Test
  void testSearchMatchingMultiplePosts() throws Exception {
    // "spring" is in post 1 (title & tag) and post 3 (content)
    mockMvc.perform(get("/posts").param("search", "spring"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void testSearchCombinedWithFilterField() throws Exception {
    // "spring" + published=true matches only post 1 (post 3 is unpublished)
    mockMvc.perform(get("/posts")
            .param("search", "spring")
            .param("published", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title", is("Getting Started with Spring Boot")));
  }

  @Test
  void testCustomNamedSearchField() throws Exception {
    // query matches authorName
    mockMvc.perform(get("/posts").param("query", "Jane"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].authorName", is("Jane Smith")));
  }

  @Test
  void testSearchNoMatch() throws Exception {
    mockMvc.perform(get("/posts").param("search", "nonexistent-keyword"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }
}
