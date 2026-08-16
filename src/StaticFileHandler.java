import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler implements HttpHandler {

    private final String filePath;
    private final String contentType;

    public StaticFileHandler(
            String filePath,
            String contentType) {

        this.filePath = filePath;
        this.contentType = contentType;
    }

    @Override
    public void handle(
            HttpExchange exchange)
            throws IOException {

        Path path = Path.of(filePath);

        if (!Files.exists(path)) {

            String message = "File not found";

            exchange.sendResponseHeaders(
                    404,
                    message.getBytes().length
            );

            try (OutputStream os =
                         exchange.getResponseBody()) {

                os.write(message.getBytes());
            }

            return;
        }

        byte[] fileBytes =
                Files.readAllBytes(path);

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );

        exchange.sendResponseHeaders(
                200,
                fileBytes.length
        );

        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(fileBytes);
        }
    }
}