# Group Members
- Divya Nandigala
- Isabelle Hageman
- Meredith Bartel
---

# Protocols

Each protocol runs on localhost:6789 on top of http. For case of documentation and protocols, channel is synonomous with group and board.

## getPost

```localhost:6789/getPost?channel={channel}&id={id}&user={user}```

Gets one post in a channel based off of its post ID as long as the channel and post exists and the user is in that group.

### Params:

- `channel`: which message board the post should be in (handled by client)
- `id`: the post id (input by user)
- `user`: the user (handled by client)

## getPosts

```localhost:6789/getPost?channel={channel}&user={user}```

Gets all posts in a channel as long as the channel exists and the user is in that group.

### Params:

- `channel`: which message board the post should be in (handled by client)
- `user`: the user (handled by client)

## makePost

```localhost:6789/makePost?channel={channel}&user={user}&subject={subject}&content={content}```

Creates a post in a channel given a subject and content. Post date and ID will auto populate handled by the server. The user must be in a group in order to post to it.

### Params:

- `channel`: which message board the post should be in (handled by client)
- `user`: the user (handled by client)
- `subject`: the subject of the post (input by user)
- `content`: the contents of the post (input by the user)

## joinPrivate

```localhost:6789/joinPrivate?channel={channel}&user={user}```

Adds user to the channel they wish, enabling them to view and create posts.

### Params:

- `channel`: which message board user is joining (input by user)
- `user`: the user (handled by client)

## leavePrivate

```localhost:6789/joinPrivate?channel={channel}&user={user}```

Removes the user from a channel so they can no longer make or view posts

### Params:

- `channel`: which message board user is leaving (input by user)
- `user`: the user (handled by client)

## joinPublic

```localhost:6789/joinPublic?user={user}```

Adds the user to the public channel.

### Params:

- `user`: the user (handled by client)

## leavePublic

```localhost:6789/leavePublic?user={user}```

Removes the user from the public channel.

### Params:

- `user`: the user (handled by client)

## getUsers

```localhost:6789/getUsers?channel={channel}```

Returns the users in a channel

### Params:

- `channel`: the channel to return users for (handled by client routinely)

---

# Compilation and Execution Instructions

## Server (Java)

1. Open a terminal in the project directory
2. Compile the server:
   ```bash
   javac webserver/WebServer.java
   ```
3. Run the server:
   ```bash
   java webserver.WebServer
   ```
4. The server will start on port 6789

## Client (Python)

1. Open a new terminal in the project directory
2. Run the client:
   ```bash
   python client.py
   ```

---

# Usage Instructions

## Connecting to Server

After starting the client:
```
%connect localhost 6789
```
Then enter a username when prompted.

## Available Commands

### Connection & Setup
- `%connect <host> <port>` - Connect to bulletin board server
- `%exit` - Disconnect and exit the program
- `%help` - Display all available commands

### Public Board (Part 1)
- `%join` - Join the public message board
- `%post <subject> | <content>` - Post message to public board
- `%users` - List users in public board
- `%leave` - Leave the public board
- `%message <id>` - View full message content

### Private Groups (Part 2)
- `%groups` - List all available private groups
- `%groupjoin <group_name>` - Join a private group (e.g., private1)
- `%grouppost <group_name> <subject> | <content>` - Post to group
- `%groupusers <group_name>` - List users in a private group
- `%groupleave <group_name>` - Leave a private group
- `%groupmessage <group_name> <id>` - View message from a group

---

# Major Issues

## HTTP Connection Management
**Problem:** Initially tried to maintain a single persistent connection, but HTTP/1.0 closes connections after each request.

**Solution:** Modified client to create a new socket for each request while maintaining state (username, joined groups) on the client side.

