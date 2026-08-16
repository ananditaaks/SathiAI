import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatHandler implements HttpHandler {

    private final GeminiService geminiService =
            new GeminiService();

    /*
     * Stores conversation during the current
     * server session.
     */
    private static final List<String> conversation =
            new ArrayList<>();


    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        try {

            String method =
                    exchange.getRequestMethod();


            // =========================
            // GET
            // =========================

            if (method.equalsIgnoreCase("GET")) {

                String html =
                        loadHtml("");

                sendResponse(exchange, html);

                return;
            }


            // =========================
            // POST
            // =========================

            if (method.equalsIgnoreCase("POST")) {

                String formData =
                        new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8
                        );


                String question =
                        extractQuestion(formData);


                if (question.isBlank()) {

                    String html =
                            loadHtml(
                                    "<p>Please enter a question.</p>"
                            );

                    sendResponse(exchange, html);

                    return;
                }


                // =========================
                // ADD USER MESSAGE
                // =========================

                synchronized (conversation) {

                    conversation.add(
                            "User: " + question
                    );
                }


                // =========================
                // CREATE CONVERSATION
                // =========================

                String conversationText;

                synchronized (conversation) {

                    conversationText =
                            String.join(
                                    "\n\n",
                                    conversation
                            );
                }


                // =========================
                // GET GEMINI RESPONSE
                // =========================

                String answer =
                        geminiService.getAnswer(
                                conversationText
                        );


                // =========================
                // ADD AI RESPONSE
                // =========================

                synchronized (conversation) {

                    conversation.add(
                            "SathiAI: " + answer
                    );
                }


                // =========================
                // FORMAT RESPONSE
                // =========================

                String formattedAnswer =
                        markdownToHtml(answer);


                String html =
                        loadHtml(formattedAnswer);


                sendResponse(exchange, html);

                return;
            }


            // =========================
            // METHOD NOT ALLOWED
            // =========================

            String error =
                    "Method Not Allowed";


            byte[] errorBytes =
                    error.getBytes(
                            StandardCharsets.UTF_8
                    );


            exchange.sendResponseHeaders(
                    405,
                    errorBytes.length
            );


            try (OutputStream os =
                         exchange.getResponseBody()) {

                os.write(errorBytes);
            }


        } catch (Exception e) {

            e.printStackTrace();


            String error =
                    "Server Error: "
                            + e.getMessage();


            byte[] errorBytes =
                    error.getBytes(
                            StandardCharsets.UTF_8
                    );


            exchange.sendResponseHeaders(
                    500,
                    errorBytes.length
            );


            try (OutputStream os =
                         exchange.getResponseBody()) {

                os.write(errorBytes);
            }
        }
    }


    // =====================================================
    // LOAD HTML
    // =====================================================

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


        /*
         * IMPORTANT:
         *
         * Do NOT escape the answer here.
         *
         * markdownToHtml() already safely escapes
         * the original Gemini response.
         */

        html =
                html.replace(
                        "{{ANSWER}}",
                        answer
                );


        return html;
    }


    // =====================================================
    // EXTRACT QUESTION
    // =====================================================

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


    // =====================================================
    // MARKDOWN → HTML
    // =====================================================

    private String markdownToHtml(
            String markdown) {

        if (markdown == null ||
                markdown.isBlank()) {

            return "";
        }


        /*
         * STEP 1
         *
         * Escape original HTML characters.
         *
         * This keeps the response safe.
         */

        String text =
                escapeHtml(markdown);


        /*
         * STEP 2
         *
         * Temporarily remove code blocks.
         *
         * This is important because later we convert
         * new lines into <br>.
         *
         * We do NOT want <br> inside code blocks.
         */

        List<String> codeBlocks =
                new ArrayList<>();


        java.util.regex.Pattern codePattern =
                java.util.regex.Pattern.compile(
                        "(?s)```(?:[a-zA-Z0-9+#.-]+)?\\s*(.*?)```"
                );


        java.util.regex.Matcher codeMatcher =
                codePattern.matcher(text);


        StringBuffer codeBuffer =
                new StringBuffer();


        while (codeMatcher.find()) {

            String code =
                    codeMatcher.group(1);


            String placeholder =
                    "___CODE_BLOCK_"
                            + codeBlocks.size()
                            + "___";


            codeBlocks.add(code);


            codeMatcher.appendReplacement(
                    codeBuffer,
                    java.util.regex.Matcher.quoteReplacement(
                            placeholder
                    )
            );
        }


        codeMatcher.appendTail(codeBuffer);


        text =
                codeBuffer.toString();


        /*
         * STEP 3
         *
         * Headings
         */

        text =
                text.replaceAll(
                        "(?m)^###### (.+)$",
                        "<h6>$1</h6>"
                );


        text =
                text.replaceAll(
                        "(?m)^##### (.+)$",
                        "<h5>$1</h5>"
                );


        text =
                text.replaceAll(
                        "(?m)^#### (.+)$",
                        "<h4>$1</h4>"
                );


        text =
                text.replaceAll(
                        "(?m)^### (.+)$",
                        "<h3>$1</h3>"
                );


        text =
                text.replaceAll(
                        "(?m)^## (.+)$",
                        "<h2>$1</h2>"
                );


        text =
                text.replaceAll(
                        "(?m)^# (.+)$",
                        "<h1>$1</h1>"
                );


        /*
         * STEP 4
         *
         * Bold text
         */

        text =
                text.replaceAll(
                        "\\*\\*(.+?)\\*\\*",
                        "<strong>$1</strong>"
                );


        /*
         * STEP 5
         *
         * Inline code
         */

        text =
                text.replaceAll(
                        "`([^`]+)`",
                        "<code>$1</code>"
                );


        /*
         * STEP 6
         *
         * Italic text
         */

        text =
                text.replaceAll(
                        "(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)",
                        "<em>$1</em>"
                );


        /*
         * STEP 7
         *
         * Bullet points
         */

        text =
                text.replaceAll(
                        "(?m)^[-*] (.+)$",
                        "<li>$1</li>"
                );


        /*
         * STEP 8
         *
         * Numbered lists
         */

        text =
                text.replaceAll(
                        "(?m)^\\d+\\. (.+)$",
                        "<li>$1</li>"
                );


        /*
         * STEP 9
         *
         * Wrap consecutive list items.
         */

        text =
                text.replaceAll(
                        "(?s)(<li>.*?</li>(?:\\s*<li>.*?</li>)*)",
                        "<ul>$1</ul>"
                );


        /*
         * STEP 10
         *
         * Normalize line breaks.
         */

        text =
                text.replace(
                        "\r\n",
                        "\n"
                );


        /*
         * Convert double line breaks
         * into paragraph spacing.
         */

        text =
                text.replace(
                        "\n\n",
                        "<br><br>"
                );


        /*
         * Convert remaining single line breaks.
         */

        text =
                text.replace(
                        "\n",
                        "<br>"
                );


        /*
         * STEP 11
         *
         * Restore code blocks.
         */

        for (int i = 0;
             i < codeBlocks.size();
             i++) {

            String code =
                    codeBlocks.get(i);


            String codeHtml =
                    "<pre><code>"
                            + code
                            + "</code></pre>";


            text =
                    text.replace(
                            "___CODE_BLOCK_"
                                    + i
                                    + "___",
                            codeHtml
                    );
        }


        /*
         * STEP 12
         *
         * If response is ordinary text,
         * place it inside a paragraph.
         */

        if (!text.startsWith("<h1>")
                && !text.startsWith("<h2>")
                && !text.startsWith("<h3>")
                && !text.startsWith("<h4>")
                && !text.startsWith("<h5>")
                && !text.startsWith("<h6>")
                && !text.startsWith("<ul>")
                && !text.startsWith("<pre>")
                && !text.startsWith("<p>")) {

            text =
                    "<p>"
                            + text
                            + "</p>";
        }


        return text;
    }


    // =====================================================
    // ESCAPE HTML
    // =====================================================

    private String escapeHtml(String text) {

        if (text == null) {

            return "";
        }


        return text
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "<",
                        "&lt;"
                )
                .replace(
                        ">",
                        "&gt;"
                )
                .replace(
                        "\"",
                        "&quot;"
                )
                .replace(
                        "'",
                        "&#39;"
                );
    }


    // =====================================================
    // SEND RESPONSE
    // =====================================================

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