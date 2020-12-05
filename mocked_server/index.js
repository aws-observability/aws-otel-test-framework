'use strict';

const express = require('express');
const https = require('https');
const http = require('http');
const fs = require('fs');
const app = express();

let data = "";
let numTransactions = 0;
const startTime = new Date();

// Retrieve number of transactions per minute
app.get("/tpm", function (req, res) {
    // Calculate duration in minutes
    const duration = Math.ceil((new Date() - startTime) / 60000);
    const tpm = numTransactions / duration;
    res.send({ tpm });
});

app.get("/check-data", function (req, res) {
    res.send(data);
});

app.all('/put-data*', function (req, res) {
    data = "success";
    numTransactions++;
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

