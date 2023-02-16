var page = require('webpage').create();
var system = require("system");
var url = system.args[1];


page.onConsoleMessage = function (message) {
    console.log(message);
};

page.open(url, function (status) {
    page.evaluate(function(){
        hasch.test.run();
    });
    phantom.exit(0);
});
