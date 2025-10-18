package com.gpis.marketplace_link.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


public class DangerousContentDetectedService {

    private List<Pattern> pattern;

    public void loadDictionary() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    getClass().getClassLoader().getResourceAsStream("dangerous-words-dictionary.txt")
                            )
                    )
            );
            pattern = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(word -> Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el diccionario de palabras peligrosas", e);
        }

    }
    public boolean containsDangerousContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (Pattern p : pattern) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public List<String> findDangerousWords(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        return pattern.stream()
                .filter(pattern -> pattern.matcher(text).find())
                .map(Pattern::pattern)
                .toList();
    }
}
