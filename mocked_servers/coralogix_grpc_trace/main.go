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
	"io"
	"log"
	"net"
	"net/http"
	"sync"
	"time"

	pb "github.com/coralogix/opentelemetry-cx-protobuf-api/coralogixpb"
	"google.golang.org/grpc"
)

const (
	HealthCheckMessage = "healthcheck"
	SuccessMessage     = "success"
)

type traceServiceServer struct {
	mu         sync.Mutex // guards isReceived
	isReceived bool
	pb.UnimplementedCollectorServiceServer
}

func healthCheck(w http.ResponseWriter, _ *http.Request) {
	if _, err := io.WriteString(w, HealthCheckMessage); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (tss *traceServiceServer) checkData(w http.ResponseWriter, _ *http.Request) {
	var message string
	tss.mu.Lock()
	if tss.isReceived {
		message = SuccessMessage
	}
	tss.mu.Unlock()

	if _, err := io.WriteString(w, message); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

// Export Implements the RPC method.
func (tss *traceServiceServer) PostSpans(_ context.Context, req *pb.PostSpansRequest) (*pb.PostSpansResponse, error) {
	tss.mu.Lock()
	tss.isReceived = true
	tss.mu.Unlock()

	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	return &pb.PostSpansResponse{}, nil
}

// Starts an RPC server that receives requests for the data handler service at the sample server port
// Starts an HTTP server that receives request from validator only to verify the data ingestion
func main() {
	var wg sync.WaitGroup
	wg.Add(2)

	server := traceServiceServer{}

	go func(tss *traceServiceServer) {
		defer wg.Done()

		listener, err := net.Listen("tcp", ":55671")
		if err != nil {
			log.Fatalf("Failed to listen: %v", err)
		}
		dataApp := grpc.NewServer()
		pb.RegisterCollectorServiceServer(dataApp, tss)
		log.Printf("GRPC trace server listening at %v", listener.Addr())
		if err = dataApp.Serve(listener); err != nil {
			log.Fatalf("GRPC trace server error: %v", err)
		}
	}(&server)

	go func(tss *traceServiceServer) {
		defer wg.Done()

		verifyApp := http.NewServeMux()
		verifyApp.HandleFunc("/", healthCheck)
		verifyApp.HandleFunc("/check-data", tss.checkData)
		if err := http.ListenAndServe(":8080", verifyApp); err != nil {
			log.Fatalf("Verification server error: %v", err)
		}
	}(&server)

	wg.Wait()
}
