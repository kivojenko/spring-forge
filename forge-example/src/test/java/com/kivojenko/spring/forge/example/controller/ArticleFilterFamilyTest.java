package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.ArticleForgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@code @FilterField(family = …)}: every filter in one family is OR-ed with the
 * others, and the family as a whole is AND-ed with the remaining filters.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ArticleFilterFamilyTest extends WithPostgres {

  @Autowired
  private ArticleForgeRepository articleRepository;

  @BeforeEach
  void setUp() throws Exception {
    articleRepository.deleteAll();

    createArticle("Quantum computing", "A short primer", "Ada", "Science", true, "physics", "hardware");
    createArticle("Gardening basics", "Quantum leaps in tomatoes", "Bob", "Home", true, "plants");
    createArticle("Weekly roundup", "Nothing special", "Ada", "Home", true, "quantum", "news");
    createArticle("Draft notes", "Quantum draft", "Ada", "Science", false, "physics");
    createArticle("Unrelated", "Nothing special", "Bob", "Science", true, "cooking");
  }

  @AfterEach
  void tearDown() {
    articleRepository.deleteAll();
  }

  private void createArticle(
      String title, String summary, String author, String section, boolean published, String... keywords)
      throws Exception {
    var body = "{"
        + "\"title\":\"" + title + "\","
        + "\"summary\":\"" + summary + "\","
        + "\"author\":\"" + author + "\","
        + "\"section\":\"" + section + "\","
        + "\"published\":" + published + ","
        + "\"keywords\":" + jsonArray(keywords)
        + "}";

    mockMvc.perform(post("/articles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
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
  void familyMembersAreOrEdWithEachOther() throws Exception {
    // title OR summary OR keywords — one value per parameter, three different matches
    mockMvc.perform(get("/articles")
            .param("title", "quantum")
            .param("summary", "quantum")
            .param("keywords", "quantum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)))
        .andExpect(jsonPath("$.content[?(@.title == 'Quantum computing')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Gardening basics')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Weekly roundup')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Draft notes')]").exists());
  }

  @Test
  void aSingleFamilyMemberFiltersOnItsOwn() throws Exception {
    mockMvc.perform(get("/articles").param("keywords", "quantum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Weekly roundup"));

    mockMvc.perform(get("/articles").param("summary", "quantum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.title == 'Gardening basics')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Draft notes')]").exists());
  }

  @Test
  void familyIsAndEdWithFiltersOutsideIt() throws Exception {
    // published=true narrows the OR-ed family rather than joining it
    mockMvc.perform(get("/articles")
            .param("title", "quantum")
            .param("summary", "quantum")
            .param("keywords", "quantum")
            .param("published", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[?(@.title == 'Draft notes')]").doesNotExist());

    mockMvc.perform(get("/articles")
            .param("title", "quantum")
            .param("published", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void aFamilyDeclaredAsAndRequiresEveryMemberItIsGiven() throws Exception {
    // @FilterFamily(name = "attribution", matchMode = AND): both members have to match, so this is the
    // intersection (2) rather than the union an OR family would give (4)
    mockMvc.perform(get("/articles")
            .param("author", "ada")
            .param("section", "science"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.title == 'Quantum computing')]").exists())
        .andExpect(jsonPath("$.content[?(@.title == 'Draft notes')]").exists());

    // One member on its own still filters by that member alone
    mockMvc.perform(get("/articles").param("author", "ada"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));

    mockMvc.perform(get("/articles")
            .param("author", "ada")
            .param("section", "home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Weekly roundup"));
  }

  @Test
  void familiesAreAndEdWithEachOther() throws Exception {
    // (title OR keywords) AND (author AND section)
    mockMvc.perform(get("/articles")
            .param("title", "quantum")
            .param("keywords", "quantum")
            .param("author", "ada")
            .param("section", "home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Weekly roundup"));
  }

  @Test
  void anEmptyFamilyLeavesTheResultsUntouched() throws Exception {
    mockMvc.perform(get("/articles").param("published", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)));

    mockMvc.perform(get("/articles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)));
  }
}
