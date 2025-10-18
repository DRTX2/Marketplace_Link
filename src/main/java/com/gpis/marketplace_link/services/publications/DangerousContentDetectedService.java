package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.exceptions.business.publications.DangerousDictionaryLoadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class DangerousContentDetectedService {

    private List<Pattern> pattern;

    public void loadDictionary() {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dangerous-words-dictionary.txt")) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                pattern = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .map(word -> Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE))
                        .toList();
            }
        } catch (IOException e) {
            throw new DangerousDictionaryLoadException("Error de E/S al cargar el diccionario de palabras peligrosas", e);
        } catch (Exception e) {
            throw new DangerousDictionaryLoadException("Error al cargar el diccionario de palabras peligrosas", e);
        }

    }
    public boolean containsDangerousContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (pattern == null || pattern.isEmpty()) {
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
        if (pattern == null || pattern.isEmpty()) return Collections.emptyList();

        return pattern.stream()
                .filter(p -> p.matcher(text).find())
                .map(Pattern::pattern)
                .toList();
    }
}
