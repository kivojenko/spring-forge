package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.model.general.TranslationForgeController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/translations")
@RequiredArgsConstructor
@Slf4j
public class TranslationController extends TranslationForgeController {

    @GetMapping("/{locale}/{id}")
    public String getTranslation(@PathVariable String locale, @PathVariable Long id) {
        return getById(id).getTranslation(locale);
    }
}