import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private final String API_URL =
            Config.GEMINI_API_URL;

    private final String API_KEY =
            Config.GEMINI_API_KEY;

    private final String MODEL =
            Config.GEMINI_MODEL;

    private final HttpClient client =
            HttpClient.newHttpClient();


    public String getAnswer(String question) {

        try {

            if (API_KEY == null || API_KEY.isBlank()) {

                return "Error: Unable to find GEMINI_API_KEY.";
            }


            if (question == null || question.isBlank()) {

                return "Please enter a question.";
            }


            String url =
                    API_URL
                            + MODEL
                            + ":generateContent";


            String jsonBody = """ 
                    { 
                        "contents": [ 
                            { 
                                "parts": [ 
                                    { 
                                        "text": "%s" 
                                    } 
                                ] 
                            } 
                        ] 
                    } 
                    """.formatted(
                    escapeJson(question)
            );


            HttpRequest request =
                    HttpRequest.newBuilder()

                            .uri(URI.create(url))

                            .header(
                                    "Content-Type",
                                    "application/json"
                            )

                            .header(
                                    "x-goog-api-key",
                                    API_KEY
                            )

                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(jsonBody)
                            )

                            .build();


            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );


            if (response.statusCode() == 200) {

                return extractAnswer(response.body());
            }


            return "Gemini API Error: "
                    + response.statusCode()
                    + "\n"
                    + response.body();


        } catch (Exception e) {

            e.printStackTrace();

            return "Something went wrong: "
                    + e.getMessage();
        }
    }


    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    private String extractAnswer(String json) {

        String searchText =
                "\"text\":";

        int start =
                json.indexOf(searchText);


        if (start == -1) {

            return "Answer not found.\n\nGemini Response:\n"
                    + json;
        }


        start =
                start + searchText.length();


        while (
                start < json.length()
                        &&
                        Character.isWhitespace(
                                json.charAt(start)
                        )
        ) {

            start++;
        }


        if (
                start < json.length()
                        &&
                        json.charAt(start) == '"'
        ) {

            start++;
        }


        StringBuilder answer =
                new StringBuilder();

        boolean escaped = false;


        for (
                int i = start;
                i < json.length();
                i++
        ) {

            char current =
                    json.charAt(i);


            if (escaped) {

                switch (current) {

                    case 'n':
                        answer.append('\n');
                        break;

                    case 'r':
                        answer.append('\r');
                        break;

                    case 't':
                        answer.append('\t');
                        break;

                    case '"':
                        answer.append('"');
                        break;

                    case '\\':
                        answer.append('\\');
                        break;

                    default:
                        answer.append(current);
                }

                escaped = false;

            }

            else if (current == '\\') {

                escaped = true;

            }

            else if (current == '"') {

                break;

            }

            else {

                answer.append(current);
            }
        }


        return answer.toString();
    }
}