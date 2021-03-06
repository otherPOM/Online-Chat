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

The task of the client is to connect and send a unique name to the server. After that, the server should send the last 10 messages (or fewer if there are fewer than 10 messages since the start of the chat). The user should be able to type messages into the console input and at the same time receive new messages from the server. Use multithreading to achieve this behavior. To exit the chat, the user should write /exit.

1. First, the server should send the following message, and the client should print it:
`Server: write your name`
2. If this username was already used, the server should send the following message, and the client should print it:
`Server: this name is already taken! Choose another one.`