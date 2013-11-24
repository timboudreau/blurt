Blurt
=====

A handy little service for sending off fire-and-forget UDP packets with 
JSON or [BSON](http://bsonspec.org/)
payloads.  Useful for performance monitoring or as a simple message bus between 
applications or within a cluster.  Each packet contains unique ids to identify the 
host, application and installed copy of the application, in addition to the payload.

Implemented in both Java (using NIO) and [NodeJS](http://nodejs.org).


Java-Blurt
----------

Uses [Giulius](../../../giulius) for configuration and Guice for initialization -
this relies on simple properties files or Properties objects bound using Guice.

To use BSON instead of JSON, do the following in a Guice module:

		bind(BlurtCodec.class).to(BsonCodec.class);

BSON keeps packet size smaller (with IPv4, you want to keep UDP packet size under
512 bytes).

To use it, simply ask Guice to inject an instance of ``Blurt``, and then call
it's ``blurt(Object)`` method.

        @Inject
        public MyClass(Blurt blurt, SomeObject obj) {
            blurt.blurt(obj);
        }

The object must be serializable using Jackson (if you need to customize serialization,
simply bind ``ObjectMapper`` using Guice to a preconfigured one and Blurt will use
it).

To receive messages, simply bind ``BlurtReceiver`` - it will be called when there
is a message:

        public class MyReceiver extends BlurtReceiver {
            protected void receive(Message<Map<String,Object>> map) {
                System.out.println("Hey, somebody sent me this: " + map);
            }
        }

Configuration
-------------

Supports unicast, multicast or broadcast.  The following properties affect behavior - 
set them using Giulius' flexible settings mechanism (overlaid properties files in ``/etc/``,
``~/`` and ``./``):

 * ``blurt.loopback.only`` - Only talk to the loopback network interface ``127.0.0.1``
 * ``blurt.udp.port`` - The UDP port to send messages to and listen on
 * ``blurt.udp.host`` - The host to send messages to.  Default is ``224.0.0.1`` for IPv4 and ``ff02::1`` for IPv6
 * ``blurt.udp.buffer.size`` - The size of the buffer to allocate for incoming packets;  the default is 
1024 bytes.  Note that how many bytes can actually be sent or received depends on the network topology - increasing
this value beyond the maximum possible UDP packet size on the network will simply waste memory.
 * ``blurt.udp.network.interface`` - The name of the network interface to listen to - the default is the first non-loopback
interface
 * ``blurt.enabled`` - If set to false, initialize but do not actually send or receive anything
 * ``blurt.autostart`` - Call ``BlurtControl.start()`` if a message is sent and it has not been called;  default is true.
 * ``blurt.upd.ipv6`` - Use IPv6
 * ``blurt.udp.thread.count`` - Number of threads to use for handing off messages.  The I/O loop is 
single-threaded NIO; when received, messages are dispatched to the bound ``BlurtReceiver`` on a thread pool.  t
 * ``blurt.heartbeat`` - If true, send a "heartbeat" uptime packet every so often to let monitoring applications
know the application is still alive
 * ``blurt.udp.multicast.loopback`` - Do multicast on the loopback interface
 * ``blurt.udp.broadcast`` - Use UDP broadcast
 * ``blurt.udp.multicast.ttl`` - Number of hops multicast packets should live
 * ``blurt.udp.traffic.class`` - Traffic class
 * ``blurt.send`` - If set to false, do not send messages, only listen
 * ``blurt.receive`` - If set to false, do not listen for messages, only send


Maven
-----

To use it from Maven, add [the Maven repository described here](http://timboudreau.com/builds)
to your POM file, and then set a dependency, e.g.

        <dependency>
            <groupId>com.mastfrog</groupId>
            <artifactId>blurt</artifactId>
            <version>1.3.9-SNAPSHOT</version>
        </dependency>

Check the POM file linked above for the current version.


Blurt for NodeJS
----------------

The NodeJS implementation interoperates with the Java implementation.  Usage is
similarly simple.  The following defaults are comparable to the ones above;
pass an object to ``Blurt``'s constructor to override it.

        var defaults = {
            host: "224.0.0.1",
            port: 41234,
            heartbeat: true,
            interval: 10000,
            ipv6: false,
            bson: false,
            autostart: true,
            multicastLoopback: true,
            multicastTtl: 2,
            send: true,
            receive: true,
            bson: false
        };

The javascript ``Blurt`` is a standard ``EventEmitter``; like the Java version,
you pass an object to the ``blurt()`` method.

For example:

        var Blurt = require('blurt'), util = require('util');
        var blurt = new Blurt({ ipv6 : true, host : 'ff02::1'});

        blurt.on('message', function (obj, app, remoteInfo) {
            console.log("Hey, I got a " + util.inspect(obj));
        });

        blurt.blurt({ someNumber : 23, aMessage : 'hello blurt'});


Unique IDs
----------

Each blurt message contains information about the origin application - its
name, a unique ID generated and saved the first time it is run (in ``~/.$APPLICATION_NAME``),
and an "instance id" which is an ID regenerated on startup.  This, combined with
the origin IP address, is enough information to identify which application 
sent the message, which machine and which run of that application sent it.
This information is helpful if doing shared logging or performance event
collection.

The strings are simply random characters which are generated, and can be
mapped back to the originating application, and can be used to distinguish
multiple instances of the same application on the same machine or cluster.

They are also what Blurt uses, in a multicast or broadcast situation,
to avoid sending you packets that were sent from the same process they
are received in.


BSON instead of JSON
--------------------

UDP has limitations on packet size, so to fit the most information into the smallest
number of bytes, [BSON](http://bsonspec.org/) (the binary JSON format [MongoDB](http://mongodb.org) uses) is
supported, which significantly reduces the number of bytes needed for packets, particularly
in the case of strings which cost extra bytes for quotes.

To enable BSON in the Java version, bind the following with Guice:

       bind(BlurtCodec.class).to(BsonCodec.class);

To enable BSON in the Javascript version, set the ``bson`` property to ``true`` in the
configuration object you pass to the ``Blurt`` constructor.

Other codecs are possible (for example, an encrypting wrapper) are possible;  it is
important, however, to respect the length limitations of UDP packets.


What Goes Over The Wire
-----------------------

The object you pass to ``blurt()`` (which should be a ``Map``, javascript hash or something, at any rate, that
can be resolved to key/value pairs) is simply serialized to JSON or BSON, with one little
addition:  A key ``i`` is added (I know, I know, one letter variables - but we're trying to
save bytes here!) to the outgoing data, which contains the following:

          $applicationId:$instanceId:$applicationName

These are stripped out of the payload you get back, and are passed to you separately.  This
does mean that any property named ``i`` will be clobbered in payloads.


A Last Word on UDP
------------------

UDP is appropriate for things where *packet loss is not catastrophic* - it trades away
reliable delivery for message atomicity.  It's great when that is the right compromise
for the application.  It is not the right thing to use if packet loss *is* disasterous.

It would be possible to write an alternate transport (say using [ZeroMQ](http://zeromq.org/)
which would still use this library's API.  Contributions are welcome!




