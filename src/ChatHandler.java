import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChatHandler implements HttpHandler {

    private final GeminiService geminiService =
            new GeminiService();

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        try {

            String method =
                    exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {

                String html =
                        loadHtml("");

                sendResponse(exchange, html);

                return;
            }

            if (method.equalsIgnoreCase("POST")) {

                String formData =
                        new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8
                        );

                String question =
                        extractQuestion(formData);

                String answer =
                        geminiService.getAnswer(question);

                String html =
                        loadHtml(answer);

                sendResponse(exchange, html);

                return;
            }

            String error =
                    "Method Not Allowed";

            exchange.sendResponseHeaders(
                    405,
                    error.getBytes(StandardCharsets.UTF_8).length
            );

            try (OutputStream os =
                         exchange.getResponseBody()) {

                os.write(
                        error.getBytes(StandardCharsets.UTF_8)
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            String error =
                    "Server Error: " + e.getMessage();

            exchange.sendResponseHeaders(
                    500,
                    error.getBytes(StandardCharsets.UTF_8).length
            );

            try (OutputStream os =
                         exchange.getResponseBody()) {

                os.write(
                        error.getBytes(StandardCharsets.UTF_8)
                );
            }
        }
    }

    private String loadHtml(String answer)
            throws IOException {

        Path path =
                Path.of(
                        "web",
                        "index.html"
                );

        String html =
                Files.readString(
                        path,
                        StandardCharsets.UTF_8
                );

        html = html.replace(
                "{{ANSWER}}",
                escapeHtml(answer)
        );

        return html;
    }

    private String extractQuestion(
            String formData) {

        if (formData == null ||
                formData.isBlank()) {

            return "";
        }

        String[] parts =
                formData.split("&");

        for (String part : parts) {

            String[] keyValue =
                    part.split("=", 2);

            if (keyValue.length == 2 &&
                    keyValue[0].equals("message")) {

                return URLDecoder.decode(
                        keyValue[1],
                        StandardCharsets.UTF_8
                );
            }
        }

        return "";
    }

    private String escapeHtml(String text) {

        if (text == null) {

            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void sendResponse(
            HttpExchange exchange,
            String html)
            throws IOException {

        byte[] response =
                html.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                200,
                response.length
        );

        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(response);
        }
    }
}