// TODO: Protocols
// POST users 
// GET users
// POST posts(subject, content, date, user)
//handle message key in server based off of len of current post list
// GET messages
// TODO: Server init
// TODO: recieving connections
// TODO: message handling

// TODO: mock-database (aka a bunch of lists, one per chat environment)

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HTTPServer {
    static List<Post> publicPosts = new ArrayList<>();

    static class Post {
        Date date;
        String user;
        String subject;
        String contents;
        int index;

        public Post(Date date, String user, String subject, String contents, int index) {
            this.date = date;
            this.user = user;
            this.subject = subject;
            this.contents = contents;
            this.index = index;
        }

        @Override
        public String toString() {
            return "Post{" +
                    "date=" + date +
                    ", user='" + user + '\'' +
                    ", subject='" + subject + '\'' +
                    ", contents='" + contents + '\'' +
                    ", index=" + index +
                    '}';
        }
    }

    public static void main(String[] args) {
        try {
            HttpServer BE = HttpServer.create(new InetSocketAddress(6789), 0); // set port
            BE.createContext("/", new MainHandler());
            BE.createContext("/public", new PublicHandler());
            BE.createContext("/newPost", new PostHandler());
            BE.setExecutor(null);
            BE.start();
            System.out.println("Server running");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static class PostHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract parameters from query string
            String query = exchange.getRequestURI().getQuery();
            String user = "anonymous";
            String subject = "No subject";
            String contents = "No content";

            // request format http://localhost:6789/newPost?user=userinput&subject=subjectinput&content=contentinput
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) { // gets the parameters for the post from the url
                    String[] keyValue = param.split("="); 
                    if (keyValue.length == 2) {
                        switch (keyValue[0]) {
                            case "user":
                                user = keyValue[1];
                                break;
                            case "subject":
                                subject = keyValue[1];
                                break;
                            case "content":
                                contents = keyValue[1];
                                break;
                        }
                    }
                }
            }

            // Create new post
            Post newPost = new Post(
                    new Date(),
                    user,
                    subject,
                    contents,
                    publicPosts.size() + 1);
            publicPosts.add(newPost);

            String response = String.format("Post with subject %s made successfully\n", subject);
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "main page response\n";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class PublicHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int size = publicPosts.size();
            List<Post> lastTwo = publicPosts.subList(Math.max(0, size - 2), size);
            String response = "";
            if (lastTwo.size() == 0) {
                response = "No posts yet\n";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lastTwo.size(); i++) {
                    sb.append(lastTwo.get(i).toString());
                    if (i < lastTwo.size() - 1) {
                        sb.append(",");
                    }
                }

                response = sb.toString();
            }

            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}