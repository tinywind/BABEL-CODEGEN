global = {};
console = {};
console.debug = print;
console.warn = print;
console.log = print;
console.error = print;

function transform(code, preset, minified) {
    var options = {
        presets: [preset],
        minified: minified
    };
    return Babel.transform(code, options);
}