#!/usr/bin/env node
var args = process.argv.slice(2);
var TIMESTAMP = ((args[args.indexOf("--timestamp")] === ("--timestamp")) ? args[args.indexOf("--timestamp")+1] : "");
var GUARDIAN = ((args[args.indexOf("--guardian")] === ("--guardian")) ? args[args.indexOf("--guardian")+1] : "");
var dateString = (new Date(parseInt(TIMESTAMP))).toISOString().substr(0,19).replace(/:/g,"-");
var s3Path = "/"+dateString.substr(0,4)+"/"+dateString.substr(5,2)+"/"+dateString.substr(8,2)+"/"+GUARDIAN+"/"+GUARDIAN+"-"+dateString;
console.log(s3Path+'*'+(new Date(parseInt(TIMESTAMP))).toISOString()+'*');
