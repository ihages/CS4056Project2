package webserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Date;

public final class WebServer {
    public static void main(String argv[]) throws Exception {
        // Set the port number.
        int port = 6789;

        // Establish the listen socket.
        ServerSocket listenSocket = new ServerSocket(port);

        System.out.println("WebServer started on port " + port);

        // Process HTTP service requests in an infinite loop.
        while (true) {
            // Listen for a TCP connection request.
            Socket connectionSocket = listenSocket.accept();

            // Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(connectionSocket);

            // Create a new thread to process the request.
            Thread thread = new Thread(request);

            // Start the thread.
            thread.start();
        }
    }
}

final class HttpRequest implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implement the run() method of the Runnable interface.
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static class Post { // Handling of the Post type (compound type of four strings)
        Integer id;
        String user;
        String date;
        String content;
        String subject;

        public Post(Integer id, String user, String date, String content, String subject) {
            this.id = id;
            this.user = user;
            this.date = date;
            this.content = content;
            this.subject = subject;
        }

        @Override
        public String toString() {// expresses a Post as a string
            return "Post{id='" + id + "', user='" + user + "', date='" + date + "', subject='" + subject
                    + "', content='" + content + "'}";
        }
    }

    // these lists opperates as the database for the server
    private static List<Post> publicPosts = new ArrayList<>();
    private static List<Post> private1Posts = new ArrayList<>();
    private static List<Post> private2Posts = new ArrayList<>();
    private static List<Post> private3Posts = new ArrayList<>();
    private static List<Post> private4Posts = new ArrayList<>();
    private static List<Post> private5Posts = new ArrayList<>();

    // User lists for each channel
    private static List<String> publicUsers = new ArrayList<>();
    private static List<String> private1Users = new ArrayList<>();
    private static List<String> private2Users = new ArrayList<>();
    private static List<String> private3Users = new ArrayList<>();
    private static List<String> private4Users = new ArrayList<>();
    private static List<String> private5Users = new ArrayList<>();

    // Protocol handlers
    private static Map<String, String> protocols = new HashMap<>();

    static {
        // Initialize protocols dictionary
        protocols.put("getPost", "getPost");
        protocols.put("getPosts", "getPosts");
        protocols.put("makePost", "makePost");
        protocols.put("joinPrivate", "joinPrivate");
        protocols.put("leavePrivate", "leavePrivate");
        protocols.put("joinPublic", "joinPublic");
        protocols.put("leavePublic", "leavePublic");
        protocols.put("getUsers", "getUsers");
    }

    private void processRequest() throws Exception {
        // Get a reference to the socket's input and output streams.
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Set up input stream filters.
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // Get the request line of the HTTP request message.
        String requestLine = br.readLine();

        // Display the request line.
        System.out.println();
        System.out.println(requestLine);

        // Get and display the header lines.
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Extract the method and path from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken(); // Skip method (GET or POST)
        String fullPath = tokens.nextToken(); // /getPost?channel=public&id=1

        // Parse the path and query parameters
        String path;
        Map<String, String> params = new HashMap<>();

        if (fullPath.contains("?")) {
            String[] parts = fullPath.split("\\?", 2);
            path = parts[0].substring(1); // remove leading '/'
            String queryString = parts[1];

            // Parse query parameters
            String[] paramPairs = queryString.split("&");
            for (String pair : paramPairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }
        } else {
            path = fullPath.substring(1); // remove leading '/'
        }

        // Handle protocol requests
        String response = handleProtocol(path, params);

        // Send HTTP response
        String statusLine = "HTTP/1.0 200 OK" + CRLF;
        String contentTypeLine = "Content-type: application/json" + CRLF;

        os.writeBytes(statusLine);
        os.writeBytes(contentTypeLine);
        os.writeBytes(CRLF);
        os.writeBytes(response);

        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
    }

    // determines the protocol that the user sent and maps it to a function
    private String handleProtocol(String protocol, Map<String, String> params) {
        try {
            if (!protocols.containsKey(protocol)) {
                return "{\"error\": \"Unknown protocol: " + protocol + "\"}";
            }

            switch (protocol) {
                case "getPost":
                    return GetPost(params);
                case "getPosts":
                    return GetPosts(params);
                case "makePost":
                    return MakePost(params);
                case "joinPrivate":
                    return JoinPrivate(params);
                case "leavePrivate":
                    return LeavePrivate(params);
                case "joinPublic":
                    return JoinPublic(params);
                case "leavePublic":
                    return LeavePublic(params);
                case "getUsers":
                    return GetUsers(params);
                default:
                    return "{\"error\": \"Does not exist: " + protocol + "\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"Server error: " + e.getMessage() + "\"}";
        }
    }

    // Gets a singular post based off of channel and id as JSON. needs user to check
    // that user is in chat
    private String GetPost(Map<String, String> params) {
        String channel = params.get("channel");
        String id = params.get("id");
        String user = params.get("user");

        // Check if user parameter is provided
        if (user == null) {
            return "{\"error\": \"User parameter is required\"}";
        }

        // Check if user has access to private channels
        if (channel != null
                && Arrays.asList("private1", "private2", "private3", "private4", "private5").contains(channel)) {
            List<String> channelUsers = GetChannelUsers(channel);
            if (!channelUsers.contains(user)) {
                return "{\"error\": \"Access denied. You must join " + channel + " first to read posts\"}";
            }
        }

        List<Post> posts = GetChannelPosts(channel);

        if (id != null) {
            try {
                int postId = Integer.parseInt(id);
                if (postId >= 0 && postId < posts.size()) {
                    Post post = posts.get(postId);
                    return "{\"post\": " + postToJson(post) + ", \"channel\": \""
                            + (channel != null ? channel : "public") + "\"}";
                } else {
                    return "{\"error\": \"Post not found\"}";
                }
            } catch (NumberFormatException e) {
                return "{\"error\": \"Invalid post ID\"}";
            }
        } else {
            return "{\"error\": \"Post ID required\"}";
        }
    }

    // Gets 2 most recent posts in a channel as JSON. checks user to make sure user
    // is in said chat
    private String GetPosts(Map<String, String> params) {
        String channel = params.get("channel");
        String user = params.get("user");

        // Check if user parameter is provided
        if (user == null) {
            return "{\"error\": \"User parameter is required\"}";
        }

        // Check if user has access to private channels
        if (channel != null
                && Arrays.asList("private1", "private2", "private3", "private4", "private5").contains(channel)) {
            List<String> channelUsers = GetChannelUsers(channel);
            if (!channelUsers.contains(user)) {
                return "{\"error\": \"Access denied. You must join " + channel + " first to read posts\"}";
            }
        }

        List<Post> posts = GetChannelPosts(channel);

        int start = Math.max(posts.size() - 2, 0);
        List<Post> recentPosts = posts.subList(start, posts.size());

        StringBuilder json = new StringBuilder();
        json.append("{\"posts\": [");
        for (int i = 0; i < recentPosts.size(); i++) {
            json.append(postToJson(recentPosts.get(i)));
            if (i < recentPosts.size() - 1) {
                json.append(", ");
            }
        }
        json.append("], \"channel\": \"" + (channel != null ? channel : "public") + "\"}");
        return json.toString();
    }

    // create a new post -- client will need to provide user and channel, id is
    // autogenerated by server
    private String MakePost(Map<String, String> params) {
        String channel = params.get("channel");
        String user = params.get("user");
        String subject = params.get("subject");
        String content = params.get("content");

        if (user == null || subject == null || content == null) {
            List<String> missing = new ArrayList<>();
            if (user == null)
                missing.add("user");
            if (subject == null)
                missing.add("subject");
            if (content == null)
                missing.add("content");
            return "{\"error\": \"Missing required parameters\", \"missing\": " + missing.toString() + "}";
        }

        // Check if user has access to private channels
        if (channel != null
                && Arrays.asList("private1", "private2", "private3", "private4", "private5").contains(channel)) {
            List<String> channelUsers = GetChannelUsers(channel);
            if (!channelUsers.contains(user)) {
                return "{\"error\": \"Access denied. You must join " + channel + " first to make posts\"}";
            }
        }

        List<Post> posts = GetChannelPosts(channel);

        Integer id = posts.size() + 1;

        Post newPost = new Post(id, user, new Date().toString(), content, subject);
        posts.add(newPost);

        return "{\"success\": \"Post created\", \"post\": " + postToJson(newPost) + ", \"channel\": \""
                + (channel != null ? channel : "public") + "\"}";
    }

    // User joins a specified private server (client must provide user and channel)
    private String JoinPrivate(Map<String, String> params) {
        String newUser = params.get("user");
        String channel = params.get("channel");
        if (newUser == null) {
            return "{\"error\": \"Cannot let you join a private group without your user\"}";
        }
        if (channel == null
                || !Arrays.asList("private1", "private2", "private3", "private4", "private5").contains(channel)) {
            return "{\"error\": \"Invalid or missing channel. Must specify one of: private1, private2, private3, private4, private5\"}";
        }

        List<String> channelUsers = GetChannelUsers(channel);

        // Check if user is already in the group
        if (channelUsers.contains(newUser)) {
            return "{\"message\": \"You are already in the group " + channel + "\", \"user\": \"" + newUser
                    + "\", \"channel\": \"" + channel + "\"}";
        }

        // Add user to the channel
        channelUsers.add(newUser);
        return "{\"success\": \"Successfully joined " + channel + "\", \"user\": \"" + newUser + "\", \"channel\": \""
                + channel + "\"}";
    }

    // Leave a private server (user will need to be handled by client rather
    // than by input)
    private String LeavePrivate(Map<String, String> params) {
        String leaveUser = params.get("user");
        String channel = params.get("channel");

        if (leaveUser == null) {
            return "{\"error\": \"Cannot leave without specifying user\"}";
        }
        if (channel == null || !Arrays.asList("public", "private1", "private2", "private3", "private4", "private5")
                .contains(channel)) {
            return "{\"error\": \"Invalid or missing channel\"}";
        }

        List<String> channelUsers = GetChannelUsers(channel);

        if (channelUsers.contains(leaveUser)) {
            channelUsers.remove(leaveUser);
            return "{\"success\": \"User left " + channel
                    + "\", \"user\": \"\" + leaveUser + \"\", \"channel\": \"\" + channel + \"\"}";
        }
        return "{\"error\": \"User not found in " + channel + "\"}";
    }

    // Join the public board
    private String JoinPublic(Map<String, String> params) {
        String newUser = params.get("user");
        if (newUser == null) {
            return "{\"error\": \"User parameter required\"}";
        }

        // Check if already in public
        if (publicUsers.contains(newUser)) {
            return "{\"message\": \"Already in public board\", \"user\": \"" + newUser + "\"}";
        }

        publicUsers.add(newUser);
        return "{\"success\": \"Joined public board\", \"user\": \"" + newUser + "\"}";
    }

    // Leave the public board
    private String LeavePublic(Map<String, String> params) {
        String leaveUser = params.get("user");
        if (leaveUser == null) {
            return "{\"error\": \"User parameter required\"}";
        }

        if (publicUsers.contains(leaveUser)) {
            publicUsers.remove(leaveUser);
            return "{\"success\": \"Left public board\", \"user\": \"" + leaveUser + "\"}";
        }
        return "{\"error\": \"User not in public board\"}";
    }

    // returns the list of current users in a channel (should be shared in a channel
    // when a user leaves or joins)
    private String GetUsers(Map<String, String> params) {
        String channel = params.get("channel");
        List<String> channelUsers = GetChannelUsers(channel);

        StringBuilder json = new StringBuilder();
        json.append("{\"users\": [");
        for (int i = 0; i < channelUsers.size(); i++) {
            json.append("\"").append(channelUsers.get(i)).append("\"");
            if (i < channelUsers.size() - 1) {
                json.append(", ");
            }
        }
        json.append("], \"channel\": \"" + channel + "\"}");
        return json.toString();
    }

    // exists as a map for all the channels. If we want to change the name of a
    // private channel, just change the case here and leave the "private#Posts" the
    // same
    private List<Post> GetChannelPosts(String channel) {
        if (channel == null)
            channel = "public";

        switch (channel) {
            case "public":
                return publicPosts;
            case "private1":
                return private1Posts;
            case "private2":
                return private2Posts;
            case "private3":
                return private3Posts;
            case "private4":
                return private4Posts;
            case "private5":
                return private5Posts;
            default:
                return publicPosts;
        }
    }

    // exists as a map for all the channel users. Maps each channel to its user list
    private List<String> GetChannelUsers(String channel) {
        if (channel == null)
            channel = "public";

        switch (channel) {
            case null:
                return publicUsers;
            case "public":
                return publicUsers;
            case "private1":
                return private1Users;
            case "private2":
                return private2Users;
            case "private3":
                return private3Users;
            case "private4":
                return private4Users;
            case "private5":
                return private5Users;
            default:
                return publicUsers;
        }
    }

    // converts a post to a JSON string so that it can be passed through as a
    // response
    private String postToJson(Post post) {
        return "{\"user\": \"" + post.user + "\", \"date\": \"" + post.date + "\", \"subject\": \"" + post.subject
                + "\", \"content\": \"" + post.content + "\"}";
    }

}