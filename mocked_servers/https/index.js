'use strict';

const express = require('express');
const https = require('https');
const http = require('http');
const fs = require('fs');
const app = express();
const dataApp = express();

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

app.all('/', function (req, res) {
    res.send('healthcheck');
});

// Separate data app from the management app to be path agnostic
dataApp.all('/*', function (req, res) {
    data = "success";
    numTransactions++;

    setTimeout((function() {res.send("{}")}), 15);

});

// Listen on port 443 for data app
https.createServer({
    key: fs.readFileSync("./certificates/private.key"),
    cert: fs.readFileSync("./certificates/ssl/certificate.crt")
}, dataApp).listen(443, "0.0.0.0");

// Listen on port 8080 for management app
http.createServer(app).listen(8080, "0.0.0.0");
