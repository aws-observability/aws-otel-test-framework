'use strict';

const express = require('express');
const https = require('https');
const http = require('http');
const fs = require('fs');
const app = express();

var get_data = ""

app.get("/check-data", function(req, res){
    res.send(get_data);
});

app.all('/put-data*', function (req, res) {
    get_data = "success";
    console.log("received data");
    res.send('{}');
});

app.all('/', function (req, res) {
    res.send('healthcheck');
});

// listen on http and https at the same time
http.createServer(app).listen(8080, "0.0.0.0");
https.createServer({
    key: fs.readFileSync("./certificates/private.key"),
    cert: fs.readFileSync("./certificates/ssl/certificate.crt")
}, app).listen(443, "0.0.0.0");

