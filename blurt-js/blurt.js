const events = require('events'), util = require('util'),
        dgram = require('dgram');

// Default values if not supplied in the config
const defaults = {
    host: "224.0.0.1",
    port: 41234,
    heartbeat: true,
    interval: 30000,
    ipv6: false,
    bson: false,
    autostart: true,
    multicastLoopback: true,
    multicastTtl: 2,
    send: true,
    receive: true,
};

function Blurt(suppliedConfig) {
    events.EventEmitter.call(this);
    var self = this;
    var config = {};
    for (var key in defaults) {
        config[key] = defaults[key];
    }
    if (suppliedConfig) {
        for (var key in suppliedConfig) {
            config[key] = suppliedConfig[key];
        }
    }
    if (!config['appName'] || !config['appId'] || !config['instanceId']) {
        var Info = require('./applicationInfo');
        var info = new Info();
        for (var key in info) {
            if (!config[key]) {
                config[key] = info[key];
            }
        }
    }
    if (config.ipv6 && config.host === '224.0.0.1') {
        config.host = 'ff02::1';
    }
    var client = config.send ? dgram.createSocket(config.ipv4 ? "udp4" : "udp6") : null;

    var bson = config.bson ? require('buffalo') : null;

    var uid = config.appId + ':' + config.instanceId + ':' + config.appName;
    var logged = false;
    function blurt(message, callback) {
        if (!client) {
            if (!logged) {
                console.log("Blurt send not enabled");
            }
            logged = true;
            let err = new Error('Send not enabled');
            if(callback) {
                return callback(err);
            } else {
                throw err;
            }
        }
        message.i = uid;
        var data = bson ? bson.serialize(message) : new Buffer(JSON.stringify(message));
        client.send(data, 0, data.length, config.port, config.host, function(err) {
            self.emit('sent', message);
            if (callback) {
                callback(err);
            } else if (err) {
                console.log(err);
            }
        });
    }

    if (config.heartbeat && config.send) {
        var start = new Date().getTime();
        setInterval(function() {
            var ival = new Date().getTime() - start;
            blurt({uptime: ival});
        }, config.interval);
    }

    var rex = /^(.*?):(.*?):(.*?)$/;
    if (config.receive) {
        client.bind(config.port);
        client.on('message', function(msg, rinfo) {
            try {
                var decoded = bson ? bson.parse(msg) : JSON.parse(msg);
                var i = decoded['i'];
                if (i && rex.test(i)) {
                    var inf = rex.exec(i);
                    if (uid !== decoded['i']) { // ignore our own messages
                        delete decoded['i'];
                        var info = {
                            appName: inf[3],
                            instanceId: inf[2],
                            appId: inf[1]
                        };
                        self.emit('message', decoded, info, rinfo);
                    }
                }
            } catch (err) {
                console.log(msg + ' ' + err);
            }
        });
    }

    this.blurt = blurt;
}
util.inherits(Blurt, events.EventEmitter);

module.exports = Blurt;

if (require.main === module) {
    var b = new Blurt({ipv6: true, host: 'ff02::1', bson: true});
    var i = 0;
    setInterval(function() {
        b.blurt({hello: 'hello', ix: i++});
    }, 1000);

    b.on('message', function(msg, app, rinfo) {
        console.log("RECEIVED: " + util.inspect(msg) + " from " + util.inspect(app));
        console.log(util.inspect(rinfo))
    });
}
