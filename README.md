# Online Chat https://hyperskill.org/projects/49

## Stage 1
The first stage of the project is pretty simple. Imagine that some people are messaging each other in a group chat. Your program should receive raw messages from the standard input, process them, and output only the text type messages ignoring the technical messages like when someone **joined/left** the chat.

## Stage 2
In this stage, you will implement the simplest connection between one server and one client. The client and the server get a text from the standard input. Both the client and the server should send the received messages to each other and print them. When the client disconnects, the server should terminate.

## Stage 3
Improve the server so that it can handle more than one user. To make it possible for multiple people to join, you need multithreading. The main server thread should only listen to the incoming client connections. When a client is connected, the appropriate input and output streams should be handled in a separate thread so that they don’t block other incoming clients.

