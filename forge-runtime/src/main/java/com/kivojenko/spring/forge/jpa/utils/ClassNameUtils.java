package com.kivojenko.spring.forge.jpa.utils;

import com.kivojenko.spring.forge.jpa.contract.ForgeController;
import com.kivojenko.spring.forge.jpa.contract.ForgeService;
import com.kivojenko.spring.forge.jpa.contract.HasNameRepository;
import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassNameUtils {
    private static final String BIND_ANNOTATION = "org.springframework.web.bind.annotation";

    public static final ClassName GET_MAPPING = ClassName.get(BIND_ANNOTATION, "GetMapping");
    public static final ClassName POST_MAPPING = ClassName.get(BIND_ANNOTATION, "PostMapping");
    public static final ClassName PUT_MAPPING = ClassName.get(BIND_ANNOTATION, "PutMapping");
    public static final ClassName DELETE_MAPPING = ClassName.get(BIND_ANNOTATION, "DeleteMapping");

    public static final ClassName REQUEST_MAPPING = ClassName.get(BIND_ANNOTATION, "RequestMapping");

    public static final ClassName REST_CONTROLLER = ClassName.get(BIND_ANNOTATION, "RestController");
    public static final ClassName PATH_VARIABLE = ClassName.get(BIND_ANNOTATION, "PathVariable");
    public static final ClassName REQUEST_PARAM = ClassName.get(BIND_ANNOTATION, "RequestParam");
    public static final ClassName REQUEST_BODY = ClassName.get(BIND_ANNOTATION, "RequestBody");
    public static final ClassName RESPONSE_STATUS = ClassName.get(BIND_ANNOTATION, "ResponseStatus");

    public static final ClassName DATA_INTEGRITY_VIOLATION_EXCEPTION = ClassName.get(
            "org.springframework.dao",
            "DataIntegrityViolationException"
    );

    public static final ClassName TRANSACTIONAL = ClassName.get(
            "org.springframework.transaction.annotation",
            "Transactional"
    );
    public static final ClassName AUTOWIRED = ClassName.get(
            "org.springframework.beans.factory.annotation",
            "Autowired"
    );

    public static final ClassName PAGE = ClassName.get("org.springframework.data.domain", "Page");
    public static final ClassName PAGEABLE = ClassName.get("org.springframework.data.domain", "Pageable");
    public static final ClassName PAGEABLE_DEFAULT = ClassName.get("org.springframework.data.web", "PageableDefault");

    public static final ClassName JPA_REPOSITORY = ClassName.get(
            "org.springframework.data.jpa.repository",
            "JpaRepository"
    );
    public static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");

    public static final ClassName FORGE_CONTROLLER = ClassName.get(ForgeController.class);
    public static final ClassName FORGE_SERVICE = ClassName.get(ForgeService.class);
    public static final ClassName HAS_NAME_REPOSITORY = ClassName.get(HasNameRepository.class);

    public static final ClassName GETTER = ClassName.get("lombok", "Getter");
    public static final ClassName SETTER = ClassName.get("lombok", "Setter");
    public static final ClassName TO_STRING = ClassName.get("lombok", "ToString");
    public static final ClassName BUILDER = ClassName.get("lombok", "Builder");
    public static final ClassName BUILDER_DEFAULT = ClassName.get("lombok", "Builder", "Default");
    public static final ClassName ALL_ARGS = ClassName.get("lombok", "AllArgsConstructor");
    public static final ClassName REQUIRED_ARGS = ClassName.get("lombok", "RequiredArgsConstructor");

    public static final ClassName QUERY_DSL_PREDICATE_EXECUTOR = ClassName.get(
            "org.springframework.data.querydsl",
            "QuerydslPredicateExecutor"
    );

    public static final ClassName ITERABLE = ClassName.get(Iterable.class);
    public static final ClassName SET = ClassName.get(Set.class);
    public static final ClassName HASH_SET = ClassName.get(HashSet.class);
    public static final ClassName LIST = ClassName.get(List.class);
    public static final ClassName ARRAY_LIST = ClassName.get(ArrayList.class);
    public static final ClassName STRING = ClassName.get(String.class);
    public static final ClassName BOOLEAN = ClassName.get(Boolean.class);
}
