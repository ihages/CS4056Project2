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

## joinPublic

```localhost:6789/leavePublic?user={user}```

Removes the user from the public channel.

### Params:

- `user`: the user (handled by client)

## getUsers

```localhost:6789/getUsers?channel={channel}```

Returns the users in a channel

### Params:

- `channel`: the channel to return users for (handled by client routinely)
