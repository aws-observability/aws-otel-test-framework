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
	"encoding/json"
	"io"
	"log"
	"net/http"
	"sync"
	"time"
)

const (
	HealthCheckMessage = "healthcheck"
	SuccessMessage     = "success"
	CertFilePath       = "./certificates/ssl/certificate.crt"
	KeyFilePath        = "./certificates/private.key"
)

type transactionStore struct {
	mu           sync.Mutex // guards data and transactions
	transactions int
	startTime    time.Time
	data         string
}

type transactionPayload struct {
	tpm int
}

func healthCheck(w http.ResponseWriter, _ *http.Request) {
	if _, err := io.WriteString(w, HealthCheckMessage); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (h *transactionStore) checkData(w http.ResponseWriter, _ *http.Request) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if _, err := io.WriteString(w, h.data); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (h *transactionStore) dataReceived(w http.ResponseWriter, _ *http.Request) {
	h.mu.Lock()
	defer h.mu.Unlock()

	h.data = SuccessMessage
	h.transactions++

	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	w.WriteHeader(http.StatusOK)
}

// Retrieve number of transactions per minute
func (h *transactionStore) tpm(w http.ResponseWriter, _ *http.Request) {
	h.mu.Lock()
	defer h.mu.Unlock()

	// Calculate duration in minutes
	now := time.Now()
	duration := now.Sub(h.startTime)
	tpm := h.transactions / int(duration.Minutes())
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(transactionPayload{tpm}); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

// Starts an HTTPS server that receives requests for the data handler service at the sample server port
// Starts an HTTP server that receives request from validator only to verify the data ingestion
func main() {
	var wg sync.WaitGroup
	wg.Add(2)

	store := transactionStore{
		data:         "",
		transactions: 0,
		startTime:    time.Now(),
	}

	go func(s *transactionStore) {
		defer wg.Done()

		dataApp := http.NewServeMux()
		dataApp.HandleFunc("/put-data/", s.dataReceived)
		dataApp.HandleFunc("/trace/v1", s.dataReceived)
		dataApp.HandleFunc("/metric/v1", s.dataReceived)
		if err := http.ListenAndServeTLS(":443", CertFilePath, KeyFilePath, dataApp); err != nil {
			log.Fatalf("HTTPS server error: %v", err)
		}
	}(&store)

	go func(s *transactionStore) {
		defer wg.Done()

		verifyApp := http.NewServeMux()
		verifyApp.HandleFunc("/", healthCheck)
		verifyApp.HandleFunc("/check-data", s.checkData)
		verifyApp.HandleFunc("/tpm", s.tpm)
		if err := http.ListenAndServe(":8080", verifyApp); err != nil {
			log.Fatalf("Verification server error: %v", err)
		}
	}(&store)

	wg.Wait()
}
