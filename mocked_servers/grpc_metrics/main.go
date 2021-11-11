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

	pb "go.opentelemetry.io/proto/otlp/collector/metrics/v1"
	"google.golang.org/grpc"
)

const (
	HealthCheckMessage = "healthcheck"
	SuccessMessage     = "success"
)

type server struct {
	data *string
	pb.UnimplementedMetricsServiceServer
}

type checkDataHandler struct {
	data *string
}

func healthCheck(w http.ResponseWriter, _ *http.Request) {
	if _, err := w.Write([]byte(HealthCheckMessage)); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (h *checkDataHandler) ServeHTTP(w http.ResponseWriter, _ *http.Request) {
	if _, err := w.Write([]byte(*h.data)); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

// Export Implements the RPC method.
func (s *server) Export(_ context.Context, _ *pb.ExportMetricsServiceRequest) (*pb.ExportMetricsServiceResponse, error) {
	*s.data = SuccessMessage
	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	return &pb.ExportMetricsServiceResponse{}, nil
}

// Starts an RPC server that receives requests for the data handler service at the sample server port
// Starts an HTTP server that receives request from validator only to verify the data ingestion
func main() {
	data := ""
	listener, err := net.Listen("tcp", ":55671")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterMetricsServiceServer(s, &server{data: &data})
	log.Printf("GRPC server listening at %v", listener.Addr())
	go log.Fatal(s.Serve(listener))

	app := http.NewServeMux()
	app.HandleFunc("/", healthCheck)
	// Implements check-data for validator.
	app.Handle("/check-data", &checkDataHandler{data: &data})
	log.Fatal(http.ListenAndServe(":8080", app))
}
