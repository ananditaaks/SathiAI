import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Config {

    public static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    public static final String GEMINI_MODEL =
            "gemini-3.5-flash-lite";

    public static final String GEMINI_API_KEY =
            loadApiKey();

    private static String loadApiKey() {

        // First: Render / system environment variable
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        // Second: .env file for local development
        Path envFile = Path.of(".env");

        if (!Files.exists(envFile)) {
            throw new RuntimeException(
                    "GEMINI_API_KEY not found. " +
                            "Set GEMINI_API_KEY as an environment variable " +
                            "or create a .env file."
            );
        }

        try {

            List<String> lines = Files.readAllLines(envFile);

            for (String line : lines) {

                line = line.trim();

                // Ignore empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Ignore comments
                if (line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("GEMINI_API_KEY=")) {

                    String key = line.substring(
                            "GEMINI_API_KEY=".length()
                    ).trim();

                    // Remove quotes if present
                    if (key.startsWith("\"") &&
                            key.endsWith("\"")) {

                        key = key.substring(
                                1,
                                key.length() - 1
                        );
                    }

                    if (key.isBlank()) {
                        throw new RuntimeException(
                                "GEMINI_API_KEY is empty."
                        );
                    }

                    return key;
                }
            }

            throw new RuntimeException(
                    "GEMINI_API_KEY not found in .env."
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to read .env file.",
                    e
            );
        }
    }

    private Config() {
        // Prevent object creation
    }
}