#!/usr/bin/env node

var args = process.argv.slice(2);

var dtFileName = ((args[args.indexOf("--filename")] === ("--filename")) ? args[args.indexOf("--filename")+1] : "");

var dtStr = dtFileName.substr(dtFileName.indexOf("-")+1);

var dtObj = new Date();
dtObj.setYear(parseInt(dtStr.split("T")[0].split("-")[0]));
dtObj.setUTCMonth(parseInt(dtStr.split("T")[0].split("-")[1])-1);
dtObj.setUTCDate(parseInt(dtStr.split("T")[0].split("-")[2]));

dtObj.setUTCHours(parseInt(dtStr.split("T")[1].split("-")[0]));
dtObj.setUTCMinutes(parseInt(dtStr.split("T")[1].split("-")[1]));
dtObj.setUTCSeconds(parseInt(dtStr.split("T")[1].split("-")[2]));
dtObj.setUTCMilliseconds(0);

console.log(dtObj.valueOf()+"");
