const express = require('express');
const http = require('http');
const app = express();

const PROTO_PATH = __dirname + '/metrichandler.proto';
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
const data_handler_proto = grpc.loadPackageDefinition(packageDefinition).opentelemetry.proto.collector.metrics.v1;
let get_data = "";

/**
 * Implements the Export RPC method.
 */
function Export(call, callback) {
    get_data = "success";
    callback(null, {message: "Export Data!"});
}

/**
 * Implements http check-data for validator.
 */
app.get("/check-data", function (req, res) {
    res.send(get_data);
});

/**
 * Starts an RPC server that receives requests for the data handler service at the sample server port
 * Starts an HTTP server that receives request from validator only to verify the data ingestion
 */
function main() {
    const server = new grpc.Server();
    server.addService(data_handler_proto.MetricsService.service, {Export: Export});
    server.bindAsync('0.0.0.0:55670', grpc.ServerCredentials.createInsecure(), () => {
        server.start();
    });
    http.createServer(app).listen(8080, "0.0.0.0");
}

main();