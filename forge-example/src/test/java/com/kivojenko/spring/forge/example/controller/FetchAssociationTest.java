package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.fetch.FetchCategory;
import com.kivojenko.spring.forge.example.model.fetch.FetchChild;
import com.kivojenko.spring.forge.example.model.fetch.FetchMeta;
import com.kivojenko.spring.forge.example.model.fetch.FetchNote;
import com.kivojenko.spring.forge.example.model.fetch.FetchOwner;
import com.kivojenko.spring.forge.example.model.fetch.FetchParent;
import com.kivojenko.spring.forge.example.model.fetch.FetchSecret;
import com.kivojenko.spring.forge.example.model.fetch.FetchTag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A view on an association endpoint makes the generated read load what the view writes in a fixed number of
 * queries: the count must be the same for 3 children as for 30.
 */
@SpringBootTest(properties = "logging.level.org.hibernate.SQL=DEBUG")
public class FetchAssociationTest extends WithPostgres {

  @Autowired EntityManagerFactory emf;
  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;

  private Long seed(String name, int count) {
    return tx.execute(status -> {
      var parent = FetchParent.builder().name(name).build();
      em.persist(parent);
      var root = category(name + " root", null);
      var middle = category(name + " middle", root);
      for (int i = 0; i < count; i++) {
        var author = new FetchOwner();
        author.setName(name + " author " + i);
        em.persist(author);
        var tag = tag(name + " tag " + i);
        var label = tag(name + " label " + i);
        var note = new FetchNote();
        note.setName("note " + i);
        em.persist(note);
        var secret = new FetchSecret();
        secret.setName("secret " + i);
        em.persist(secret);

        var child = FetchChild.builder()
            .title(name + " child " + i)
            .parent(parent)
            .category(category(name + " leaf " + i, middle))
            .author(author)
            .tags(new HashSet<>(Set.of(tag)))
            .meta(FetchMeta.builder().source("src " + i).labels(new HashSet<>(Set.of(label))).build())
            .notes(new HashSet<>(Set.of(note)))
            .secrets(new HashSet<>(Set.of(secret)))
            .build();
        parent.getChildren().add(child);
      }
      em.persist(parent);
      return parent.getId();
    });
  }

  private FetchCategory category(String name, FetchCategory parent) {
    var category = new FetchCategory();
    category.setName(name);
    category.setParent(parent);
    em.persist(category);
    return category;
  }

  private FetchTag tag(String name) {
    var tag = new FetchTag();
    tag.setName(name);
    em.persist(tag);
    return tag;
  }

  private long statements(Long id) {
    var statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    try {
      mockMvc.perform(get("/fetchParents/{id}/children", id)).andExpect(status().isOk());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return statistics.getPrepareStatementCount();
  }

  @Test
  void queryCountDoesNotGrowWithTheNumberOfChildren() {
    var small = seed("small", 3);
    var large = seed("large", 30);

    statements(small); // warm up
    var few = statements(small);
    var many = statements(large);
    System.out.println("FETCH STATEMENTS small=" + few + " large=" + many);
    assertEquals(few, many, "the number of statements must not depend on the number of children");
  }

  @Test
  void writesWhatTheViewShowsAndNothingElse() throws Exception {
    var id = seed("shape", 4);
    mockMvc
        .perform(get("/fetchParents/{id}/children", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[0].category.parent.parent.name").value("shape root"))
        .andExpect(jsonPath("$[0].author.name").exists())
        .andExpect(jsonPath("$[0].tags", hasSize(1)))
        .andExpect(jsonPath("$[0].meta.labels", hasSize(1)))
        .andExpect(jsonPath("$[0].notes").doesNotExist())
        .andExpect(jsonPath("$[0].secrets").doesNotExist());
  }

  @Test
  void unknownOwnerIsNotFoundAndNoChildrenIsEmpty() throws Exception {
    var empty = tx.execute(status -> {
      var parent = FetchParent.builder().name("empty").build();
      em.persist(parent);
      return parent.getId();
    });
    mockMvc.perform(get("/fetchParents/{id}/children", empty)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
      var result = mockMvc.perform(get("/fetchParents/{id}/children", 999999L)).andReturn();
      if (result.getResolvedException() != null) throw result.getResolvedException();
      if (result.getResponse().getStatus() >= 400) throw new IllegalStateException("status " + result.getResponse().getStatus());
    });
  }
}
