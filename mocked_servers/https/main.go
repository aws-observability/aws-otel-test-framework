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
	"log"
	"net/http"
	"time"
)

const (
	HealthCheckMessage = "healthcheck"
	SuccessMessage     = "success"
)

type checkDataHandler struct {
	data *string
}

type dataReceivedHandler struct {
	data         *string
	transactions *int
}

type tpmHandler struct {
	startTime    time.Time
	transactions *int
}

type tpmPayload struct {
	tpm int
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

// Separate data app from management app to be path agnostic
func (h *dataReceivedHandler) ServeHTTP(w http.ResponseWriter, _ *http.Request) {
	*h.data = SuccessMessage
	*h.transactions++

	// Built-in latency
	time.Sleep(15 * time.Millisecond)
	w.WriteHeader(http.StatusOK)
}

// Retrieve number of transactions per minute
func (h *tpmHandler) ServeHTTP(w http.ResponseWriter, _ *http.Request) {
	// Calculate duration in minutes
	now := time.Now()
	duration := now.Sub(h.startTime)
	tpm := *h.transactions / int(duration.Minutes())
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(tpmPayload{tpm}); err != nil {
		log.Printf("Unable to write response: %v", err)
	}
}

func main() {
	data := ""
	transactions := 0
	startTime := time.Now()
	app := http.NewServeMux()
	dataApp := http.NewServeMux()

	app.HandleFunc("/", healthCheck)
	app.Handle("/check-data", &checkDataHandler{data: &data})
	app.Handle("/tpm", &tpmHandler{startTime: startTime, transactions: &transactions})

	dataApp.Handle("/put-data*", &dataReceivedHandler{data: &data, transactions: &transactions})
	dataApp.Handle("/trace/v1", &dataReceivedHandler{data: &data, transactions: &transactions})
	dataApp.Handle("/metric/v1", &dataReceivedHandler{data: &data, transactions: &transactions})

	go log.Fatal(http.ListenAndServeTLS(":443", "./certificates/ssl/certificate.crt", "./certificates/private.key", dataApp))

	log.Fatal(http.ListenAndServe(":8080", app))
}
