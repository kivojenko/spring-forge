package com.kivojenko.spring.forge.jpa.utils;

import com.kivojenko.spring.forge.jpa.contract.ForgeController;
import com.kivojenko.spring.forge.jpa.contract.ForgeService;
import com.kivojenko.spring.forge.jpa.contract.HasNameRepository;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ClassNameUtils {
  String BIND_ANNOTATION = "org.springframework.web.bind.annotation";

  ClassName GET_MAPPING = ClassName.get(BIND_ANNOTATION, "GetMapping");
  ClassName POST_MAPPING = ClassName.get(BIND_ANNOTATION, "PostMapping");
  ClassName PUT_MAPPING = ClassName.get(BIND_ANNOTATION, "PutMapping");
  ClassName DELETE_MAPPING = ClassName.get(BIND_ANNOTATION, "DeleteMapping");

  ClassName REQUEST_MAPPING = ClassName.get(BIND_ANNOTATION, "RequestMapping");

  ClassName REST_CONTROLLER = ClassName.get(BIND_ANNOTATION, "RestController");
  ClassName PATH_VARIABLE = ClassName.get(BIND_ANNOTATION, "PathVariable");
  ClassName REQUEST_PARAM = ClassName.get(BIND_ANNOTATION, "RequestParam");
  ClassName REQUEST_BODY = ClassName.get(BIND_ANNOTATION, "RequestBody");
  ClassName RESPONSE_STATUS = ClassName.get(BIND_ANNOTATION, "ResponseStatus");

  ClassName HTTP_STATUS = ClassName.get("org.springframework.http", "HttpStatus");

  ClassName DATA_INTEGRITY_VIOLATION_EXCEPTION = ClassName.get(
      "org.springframework.dao",
      "DataIntegrityViolationException"
  );

  ClassName TRANSACTIONAL = ClassName.get("org.springframework.transaction.annotation", "Transactional");
  ClassName AUTOWIRED = ClassName.get("org.springframework.beans.factory.annotation", "Autowired");

  ClassName PAGE = ClassName.get("org.springframework.data.domain", "Page");
  ClassName PAGEABLE = ClassName.get("org.springframework.data.domain", "Pageable");
  ClassName PAGEABLE_DEFAULT = ClassName.get("org.springframework.data.web", "PageableDefault");

  ClassName JPA_REPOSITORY = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
  ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");

  ClassName FORGE_CONTROLLER = ClassName.get(ForgeController.class);
  ClassName FORGE_SERVICE = ClassName.get(ForgeService.class);
  ClassName HAS_NAME_REPOSITORY = ClassName.get(HasNameRepository.class);

  ClassName GETTER = ClassName.get("lombok", "Getter");
  ClassName SETTER = ClassName.get("lombok", "Setter");
  ClassName TO_STRING = ClassName.get("lombok", "ToString");
  ClassName BUILDER = ClassName.get("lombok", "Builder");
  ClassName BUILDER_DEFAULT = ClassName.get("lombok", "Builder", "Default");
  ClassName ALL_ARGS = ClassName.get("lombok", "AllArgsConstructor");
  ClassName REQUIRED_ARGS = ClassName.get("lombok", "RequiredArgsConstructor");

  ClassName QUERY_DSL_PREDICATE_EXECUTOR = ClassName.get(
      "org.springframework.data.querydsl",
      "QuerydslPredicateExecutor"
  );

  ClassName ITERABLE = ClassName.get(Iterable.class);
  ClassName SET = ClassName.get(Set.class);
  ClassName HASH_SET = ClassName.get(HashSet.class);
  ClassName LIST = ClassName.get(List.class);
  ClassName ARRAY_LIST = ClassName.get(ArrayList.class);
  ClassName STRING = ClassName.get(String.class);

  Set<TypeName> BOOLEAN_TYPES = Set.of(TypeName.BOOLEAN, ClassName.get(Boolean.class));

  Set<TypeName> NUMERIC_TYPES = Set.of(
      TypeName.BYTE,
      TypeName.SHORT,
      TypeName.INT,
      TypeName.LONG,
      TypeName.FLOAT,
      TypeName.DOUBLE,
      ClassName.get(Byte.class),
      ClassName.get(Short.class),
      ClassName.get(Integer.class),
      ClassName.get(Long.class),
      ClassName.get(Float.class),
      ClassName.get(Double.class),
      ClassName.get(BigDecimal.class),
      ClassName.get(BigInteger.class)
  );
}
