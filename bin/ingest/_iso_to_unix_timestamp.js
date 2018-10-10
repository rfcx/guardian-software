#!/usr/bin/env node
var args = process.argv.slice(2);
console.log((new Date(Date.parse(args[0]))).valueOf());
