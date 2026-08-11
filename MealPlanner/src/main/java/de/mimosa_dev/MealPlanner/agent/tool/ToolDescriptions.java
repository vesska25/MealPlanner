package de.mimosa_dev.MealPlanner.agent.tool;

import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Shared loader for the external tool-description resource files required by AI-33. */
final class ToolDescriptions {

    private ToolDescriptions() {
    }

    static String read(Resource resource) {
        try (var in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
