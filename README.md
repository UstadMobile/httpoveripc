## HTTP over Android IPC

HTTP over Android IPC allows apps to use REST APIs designed to run over http offline using 
Android over interprocess communication (IPC) using a bound messenger service.

Let's say App A offers a rest API that App B wants to access. The Offline Http Service can
serialize App B's HTTP request into an Android message, deliver it to App A, allow App A to process
the request, and then return the response to App B.

