var console = {
    log: print,
    info: print,
    debug: print,
    warn: function () {
        Java.type('java.lang.System').err.println(Array.prototype.join.call(arguments, ' '));
    }
};
console.error = console.warn;
