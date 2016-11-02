'use strict';

let {
    pc
} = require('general-bridge');

let log = console.log; // eslint-disable-line

let send = (data) => {
    window.__webView_bridge.accept(JSON.stringify(data));
};

let call = pc((handle) => {
    window.__onWebViewMessage = (data) => {
        handle(data, send);
    };
}, send, {
    subtraction: (a, b) => {
        return a - b;
    }
});

call('add', [1, 2]).then((ret) => {
    log(ret);
});
