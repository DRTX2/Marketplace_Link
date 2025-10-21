package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.exceptions.business.publications.DangerousDictionaryLoadException;
import com.gpis.marketplace_link.services.publications.valueObjects.DangerousWordMatch;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DangerousContentDetectedService {


    private List<Pattern> pattern;

    @PostConstruct
    public void loadDictionary() {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dangerous-words-dictionary.txt")) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                pattern = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .map(word -> Pattern.compile("\\b" + Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS))
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


    public List<DangerousWordMatch> findDangerousWords(String text) {
        if (text == null || text.isBlank() || pattern == null || pattern.isEmpty())
            return Collections.emptyList();

        List<DangerousWordMatch> matches = new ArrayList<>();
        for (Pattern p : pattern) {
            Matcher matcher = p.matcher(text);
            while (matcher.find()) {

                String fullWord = extractFullWord(text, matcher.start());

                String cleanPattern = p.pattern().replace("\\Q", "").replace("\\E", "");
                matches.add(new DangerousWordMatch(fullWord, cleanPattern));
            }
        }
        return matches;
    }


    private String extractFullWord(String text, int matchStart) {
        int start = matchStart;
        int end = matchStart;


        while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            start--;
        }


        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }

        return text.substring(start, end);
    }
}
