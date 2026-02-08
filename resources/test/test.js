var page = require('webpage').create();
var url = phantom.args[0];

page.onConsoleMessage = function (message) {
    console.log(message);
};

page.open(url, function (status) {
    page.evaluate(function(){
        // Use your namespace instead of `cljs-test-example`:
        hasch.test.run();
    });
    phantom.exit(0);
});

