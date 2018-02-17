# Simple DIS Gizmo

Quick and dirtly little app-hack whose purpose is to simply create
 a TCP DIS packet that can be observed with Wire Shark in a lab.

It is just a way for students to create this kind of network traffic
for an exercise.

## Misc Info

* Uses Vertx to create a net server
* Uses Vertx to create a net client
* When the student starts a server, it announces itself to local 
  network using mDNS
* Any running instance of the app that sees the mDNS advert can
  subscribe to that server, wherever it is.
* If someone clicks "Send PDU", in their instance of the app, then 
  all subscribers to that particular server will receive it. 
  (including the same app that sent it, if it is subscribed.)

The app uses TornadoFX event bus to send info to the UI from Vertx-oriented service objects (net clients and servers)

But, you could use Vertx Event bus instead of JavaFX event bus, or some combination by getting a reference to the Vertx and registering on the event bus.


### License

This is in the public domain. Use at your own risk. It probably has bugs and it comes with no warantee or claim that it will work, be safe to use, or will be useful for any purpose whatsoever.
