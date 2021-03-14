# Online Chat https://hyperskill.org/projects/49

## Stage 1
The first stage of the project is pretty simple. Imagine that some people are messaging each other in a group chat. Your program should receive raw messages from the standard input, process them, and output only the text type messages ignoring the technical messages like when someone **joined/left** the chat.

## Stage 2
In this stage, you will implement the simplest connection between one server and one client. The client and the server get a text from the standard input. Both the client and the server should send the received messages to each other and print them. When the client disconnects, the server should terminate.

## Stage 3
Improve the server so that it can handle more than one user. To make it possible for multiple people to join, you need multithreading. The main server thread should only listen to the incoming client connections. When a client is connected, the appropriate input and output streams should be handled in a separate thread so that they don’t block other incoming clients.

## Stage 4
It is time to write an actual group chat.

The first task of the server is to ask the name of every connected client. If a client with a given name already exists, the server should ask for another name. After that, when one of the clients sends a message, the server should send this message to all the connected clients. Also, when someone connects to the chat, the server should send this client the last 10 messages in the chat, so that the client has some context.

The task of the client is to connect and send a unique name to the server. After that, the server should send the last 10 messages (or fewer if there are fewer than 10 messages since the start of the chat). The user should be able to type messages into the console input and at the same time receive new messages from the server. Use multithreading to achieve this behavior. To exit the chat, the user should write `/exit`.

1. First, the server should send the following message, and the client should print it:
`Server: write your name`
2. If this username was already used, the server should send the following message, and the client should print it:
`Server: this name is already taken! Choose another one.`
   
## Stage 5
Now that we have a solid base for our chat application, it's time to improve it. In this stage, you will add functionality that will allow users to register and log in to the system. Also, allow users to choose who they're writing to and continue their correspondence. The server should send the last 10 messages of the user’s correspondence with the chosen person, and the user can write a new message. The server should save every message on a hard drive to make sure that the correspondence is safe even if the server crashes.

Let's add another great feature: saving the information about logins and passwords. This has to be done wisely so that if some hacker cracked the server and stole the file with logins and passwords, there wouldn't be a chance to use it. To achieve this, the server should save the login and the hash of the password to the file. The next time the user tries to log in, the server should take the hash of the password and compare it with the hash written in the file. Knowing the hash is not useful because you can't restore the password from it. In this project, you can use `String.hashCode()` to get the hash from the password, but in a real application, you should use something more secure, for example, SHA-256. Even if you use a hash function, `String.hashCode()` is simple and easily reversed.

First, the server should identify the user. The user can send `/auth LOGIN PASSWORD` or `/registration LOGIN PASSWORD`. If the password is less than 8 symbols during registration, the server should notify the user that the password is too short.

To list all the clients that are currently online, the client should send `/list`.

To start the correspondence, the user should input `/chat NAME`. After that, the server should send the client the last 10 messages of the correspondence with this user. After that, the user can just type some text to send, except for the commands which must start with `/`.

As previously, the users should write `/exit` to exit the chat.

Also, in this stage you need to mark messages that were sent to you when you weren't chatting with a person with the word `(new)`. The prefix `(new)` near the new message should appear only for the first time you see the message. If you are chatting with someone in realtime the messages you receive shouldn't be prefixed with `(new)`.

## Stage 6
First, let's create a moderation system. Imagine that someone is spamming messages to everyone on the server. Of course, there should be a way to kick such spammers out! To achieve that, the server should store a list of the moderators’ login information on the hard drive. When one of the moderators sends the message `/kick` NAME to the server, the user with the name NAME should be kicked out of the server for 5 minutes. During these 5 minutes, the server should reject any login attempts from this user.

Also, create an admin whose username is always "admin". The admin should be able to add and remove moderators in the system. To add a moderator, the admin needs to type `/grant NAME`, and to remove a moderator, `/revoke` NAME. Of course, the admin should be able to kick users out of the server. Note that the moderator can kick out the users but cannot kick out the other moderators. The admin can kick out both users and moderators. Nobody can kick out the admin, not even the admin!

The second major improvement involves unread messages. When a user types `/unread`, the server should send back a list of users who have sent unread messages. When the user types `/chat` NAME to go to the correspondence with the user NAME, the server should send the last 10 read messages and all unread messages on top of that. Note: the server must never send more than 25 messages at a time. After, all the messages from this conversation should be marked as read. For example, if there are 22 unread messages, the server should send them and 3 read messages on top of that. If there are 26 unread messages, the server should send the last 25 unread messages, but mark all 26 as read.

Also, let's add some useful functions for the user. Right now, it is impossible to read old messages, other than the 10 last messages. Let's fix that: if the user sends `/history N`, the server should send back N last messages of the current conversation. To protect the server from the users trying to request the last million messages, the server should send back no more than 25 messages. So, if the user asks for the last 250 messages, the server should send back a list of 25 messages: the messages 250 to 226 (inclusive) back in the conversation.

Another useful feature is statistics. If the user types `/stats`, the server should send the statistics of the current conversation. The statistics consist of the total number of messages in the conversation, the total number of messages sent by that user, and the total number of messages sent by the user you’re talking to.