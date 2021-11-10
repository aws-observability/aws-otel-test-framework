// Copyright 2021 Amazon.com, Inc. or its affiliates
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"context"
	"log"
	"net"
	"net/http"
	"time"

	pb "github.com/aws-observability/aws-otel-test-framework/mockedservers/grpcmetrics/proto"
	"google.golang.org/grpc"
)

var (
	data = ""
)

type server struct {
	pb.UnimplementedMetricsServiceServer
}

// Export Implements the RPC method.
func (s *server) Export(_ context.Context, _ *pb.Request) (*pb.ExportMetricsResponse, error) {
	data = "success"
	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	return &pb.ExportMetricsResponse{Message: "Export Data!"}, nil
}

// Starts an RPC server that receives requests for the data handler service at the sample server port
// Starts an HTTP server that receives request from validator only to verify the data ingestion
func main() {
	listener, err := net.Listen("tcp", ":55671")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterMetricsServiceServer(s, &server{})
	log.Printf("GRPC server listening at %v", listener.Addr())
	go log.Fatal(s.Serve(listener))

	app := http.NewServeMux()
	app.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte("healthcheck"))
	})
	// Implements check-data for validator.
	app.HandleFunc("/check-data", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte(data))
	})
	log.Fatal(http.ListenAndServe(":8080", app))
}
