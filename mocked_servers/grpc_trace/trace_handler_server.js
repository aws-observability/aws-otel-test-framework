const express = require('express');
const http = require('http');
const app = express();

const PROTO_PATH = __dirname + '/tracehandler.proto';
const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const packageDefinition = protoLoader.loadSync(
    PROTO_PATH,
    {
        keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true
    });
const data_handler_proto = grpc.loadPackageDefinition(packageDefinition).opentelemetry.proto.collector.trace.v1;
let get_data = "";

/**
 * Implements the Export RPC method.
 */
function Export(call, callback) {
    get_data = "success";
    setTimeout((function() {callback(null, {message: "Export Data!"})}), 15);
}

/**
 * Implements check-data for validator.
 */
app.get("/check-data", function (req, res) {
    res.send(get_data);
});

/**
 * Starts an RPC server that receives requests for the data handler service at the sample server port
 * Starts an HTTP server that receives request from validator only to verify the data ingestion
 */
function main() {
    var server = new grpc.Server();
    server.addService(data_handler_proto.TraceService.service, {Export: Export});
    server.bindAsync('0.0.0.0:55671', grpc.ServerCredentials.createInsecure(), () => {
        server.start();
    });
    http.createServer(app).listen(8080, "0.0.0.0");
}

main();