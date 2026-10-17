package org.lts.weathercoat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageFilesTest {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    @Test
    public void testLanguageFilesConsistency() throws Exception {
        Path langDir = Paths.get("src/client/resources/assets/weathercoat/lang");
        assertTrue(Files.exists(langDir), "Lang directory should exist");

        Path enUsFile = langDir.resolve("en_us.json");
        assertTrue(Files.exists(enUsFile), "en_us.json should exist");

        Map<String, String> enUsKeys = GSON.fromJson(Files.newBufferedReader(enUsFile, StandardCharsets.UTF_8), MAP_TYPE);
        assertNotNull(enUsKeys, "en_us.json keys should not be null");
        assertFalse(enUsKeys.isEmpty(), "en_us.json should not be empty");

        String[] targetLangs = new String[] { "ru_ru.json", "es_es.json", "es_mx.json" };

        for (String targetLangName : targetLangs) {
            Path targetFile = langDir.resolve(targetLangName);
            assertTrue(Files.exists(targetFile), targetLangName + " should exist");

            Map<String, String> targetKeys = GSON.fromJson(Files.newBufferedReader(targetFile, StandardCharsets.UTF_8), MAP_TYPE);
            assertNotNull(targetKeys, targetLangName + " keys should not be null");

            Set<String> missingKeys = enUsKeys.keySet().stream()
                    .filter(key -> !targetKeys.containsKey(key))
                    .collect(Collectors.toSet());

            assertTrue(missingKeys.isEmpty(), "Missing keys in " + targetLangName + ": " + missingKeys);

            Set<String> extraKeys = targetKeys.keySet().stream()
                    .filter(key -> !enUsKeys.containsKey(key))
                    .collect(Collectors.toSet());

            assertTrue(extraKeys.isEmpty(), "Extra keys in " + targetLangName + ": " + extraKeys);

            for (Map.Entry<String, String> entry : enUsKeys.entrySet()) {
                String key = entry.getKey();
                String enVal = entry.getValue();
                String targetVal = targetKeys.get(key);

                long enSpecifiers = countSpecifiers(enVal);
                long targetSpecifiers = countSpecifiers(targetVal);

                assertEquals(enSpecifiers, targetSpecifiers,
                        "Format specifier count mismatch for key '" + key + "' in " + targetLangName);
            }
        }
    }

    private long countSpecifiers(String text) {
        if (text == null) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf("%s", idx)) != -1) {
            count++;
            idx += 2;
        }
        return count;
    }
}
