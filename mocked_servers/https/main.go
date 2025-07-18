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
	"sync/atomic"
	"time"
	"os"

	"github.com/gorilla/mux"
)

const (
	HealthCheckMessage = "healthcheck"
	SuccessMessage     = "success"
	CertFilePath       = "./certificates/ssl/certificate.crt"
	KeyFilePath        = "./certificates/private.key"
	HTTPSPort          = ":55671" // Changed from :443 to :55671
	HTTPPort           = ":8080"
)

type transactionStore struct {
	transactions uint32
	startTime    time.Time
}

type TransactionPayload struct {
	TransactionsPerMinute float64 `json:"tpm"`
}

func healthCheck(w http.ResponseWriter, r *http.Request) {
	log.Printf("Health check request received on: %s", r.URL.Path)
	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	if _, err := io.WriteString(w, HealthCheckMessage); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (ts *transactionStore) checkData(w http.ResponseWriter, _ *http.Request) {
	var message string
	if atomic.LoadUint32(&ts.transactions) > 0 {
		message = SuccessMessage
	}

	if _, err := io.WriteString(w, message); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func (ts *transactionStore) dataReceived(w http.ResponseWriter, r *http.Request) {
	log.Printf("Received request on path: %s", r.URL.Path)

	body, err := io.ReadAll(r.Body)
	if err != nil {
		log.Printf("Error reading request body: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	log.Printf("Received data: %s", string(body))

	atomic.AddUint32(&ts.transactions, 1)

	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	w.WriteHeader(http.StatusOK)
	log.Printf("Successfully processed request")
}

// Retrieve number of transactions per minute
func (ts *transactionStore) tpm(w http.ResponseWriter, _ *http.Request) {
	// Calculate duration in minutes
	duration := time.Now().Sub(ts.startTime)
	transactions := float64(atomic.LoadUint32(&ts.transactions))
	tpm := transactions / duration.Minutes()

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(TransactionPayload{tpm}); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

// Starts an HTTPS server that receives requests for the data handler service at the sample server port
// Starts an HTTP server that receives request from validator only to verify the data ingestion
func main() {
	var wg sync.WaitGroup
	wg.Add(2)

	store := transactionStore{startTime: time.Now()}

	go func(ts *transactionStore) {
		defer wg.Done()

		dataApp := mux.NewRouter()
		dataApp.HandleFunc("/health", healthCheck).Methods("GET")
		dataApp.PathPrefix("/put-data").HandlerFunc(ts.dataReceived)
		dataApp.HandleFunc("/trace/v1", ts.dataReceived)
		dataApp.HandleFunc("/metric/v1", ts.dataReceived)
		log.Printf("Starting HTTPS server on port: %v", HTTPSPort)
		if err := http.ListenAndServeTLS(HTTPSPort, CertFilePath, KeyFilePath, dataApp); err != nil {
			log.Printf("Failed to start HTTPS server: %v", err)
			// Check if certificate files exist
			if _, err := os.Stat(CertFilePath); os.IsNotExist(err) {
				log.Printf("Certificate file not found: %s", CertFilePath)
			}
			if _, err := os.Stat(KeyFilePath); os.IsNotExist(err) {
				log.Printf("Key file not found: %s", KeyFilePath)
			}
			log.Fatalf("HTTPS server error: %v", err)
		}
	}(&store)

	go func(ts *transactionStore) {
		defer wg.Done()

		verifyApp := http.NewServeMux()
		verifyApp.HandleFunc("/health", healthCheck)
		verifyApp.HandleFunc("/check-data", ts.checkData)
		verifyApp.HandleFunc("/tpm", ts.tpm)
		log.Printf("Starting HTTP verification server on port %v", HTTPPort)
		if err := http.ListenAndServe(HTTPPort, verifyApp); err != nil {
			log.Fatalf("Verification server error: %v", err)
		}
	}(&store)

	wg.Wait()
}
