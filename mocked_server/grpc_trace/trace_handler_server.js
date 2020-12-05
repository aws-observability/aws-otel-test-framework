
var PROTO_PATH = __dirname + '/tracehandler.proto';

var grpc = require('@grpc/grpc-js');
var protoLoader = require('@grpc/proto-loader');
var packageDefinition = protoLoader.loadSync(
    PROTO_PATH,
    {keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true
    });
var data_handler_proto = grpc.loadPackageDefinition(packageDefinition).opentelemetry.proto.collector.trace.v1;
var get_data = ""

/**
 * Implements the Export RPC method.
 */
function Export(call, callback) {
    get_data = "success";
    callback(null, {message: "Export Data!"});
}

/**
 * Implements the CheckData RPC method.
 */
function CheckData(call, callback) {
    callback(null, {message: get_data});
}

/**
 * Starts an RPC server that receives requests for the data handler service at the
 * sample server port
 */
function main() {
    var server = new grpc.Server();
    server.addService(data_handler_proto.TraceService.service, {Export: Export, CheckData: CheckData});
    server.bindAsync('0.0.0.0:55670', grpc.ServerCredentials.createInsecure(), () => {
        server.start();
    });
}

main();