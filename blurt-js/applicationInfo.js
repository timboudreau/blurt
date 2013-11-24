var fs = require('fs'), path = require('path');
function randomInt(max) {
    return Math.floor(Math.random() * max);
}
function getUserHome() {
    return process.env[(process.platform == 'win32') ? 'USERPROFILE' : 'HOME'];
}
function randomString(count) {
    var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz"
    count = count || 5;
    var result = "";
    var len = chars.length;
    for (var i = 0; i < count; i++) {
        result += chars[randomInt(len)]
    }
    return result;
}
function Info(writeFile) {
    if (typeof writeFile === 'undefined') {
        writeFile = true;
    }
    var self = this;
    self.appName = path.basename(require.main.filename);
    if (/(.*)\.js$/.test(self.appName)) {
        self.appName = /(.*)\.js$/.exec(self.appName)[1];
    }
    var guidFile = path.join(getUserHome(), '.' + self.appName);
    console.log("GuidFile " + guidFile);
    if (fs.existsSync(guidFile)) {
        self.appId = fs.readFileSync(guidFile, {encoding: 'utf8'})
    } else {
        self.appId = randomString(4);
        if (writeFile) {
            fs.writeFileSync(guidFile, self.appId);
        }
    }
    self.instanceId = randomString(5);
}
module.exports = Info;

if (require.main === module) {
    console.log(JSON.stringify(new Info()))
}
