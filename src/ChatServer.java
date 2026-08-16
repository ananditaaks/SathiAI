import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ChatServer {

    private HttpServer server;

    public void start() {

        try {

            int port = Integer.parseInt(
                    System.getenv().getOrDefault("PORT", "8081")
            );

            server = HttpServer.create(
                    new InetSocketAddress("0.0.0.0", port),
                    0
            );

            server.createContext(
                    "/chatbot",
                    new ChatHandler()
            );

            server.createContext(
                    "/style.css",
                    new StaticFileHandler(
                            "web/style.css",
                            "text/css"
                    )
            );

            server.start();

            System.out.println("=================================");
            System.out.println("Gemini Chatbot Server Started!");
            System.out.println(
                    "Open: http://localhost:" + port + "/chatbot"
            );
            System.out.println("=================================");

        } catch (IOException e) {

            System.out.println("Unable to start Server.");

            e.printStackTrace();
        }
    }
}