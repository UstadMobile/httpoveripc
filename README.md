## HTTP over Android IPC

HTTP over Android IPC allows apps to use REST APIs designed to run online over http within an 
Android device to facilitate communication between apps.

Let's say App A offers an API that App B wants to access. HttpOverAndroidIpc can serialize App B's 
REST HTTP request into an Android message, deliver it to App A, allow App A to process the request, 
and then return the response to App B.

HTTP over Android IPC even offers an embedded HTTP 

