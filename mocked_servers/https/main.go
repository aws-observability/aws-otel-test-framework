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

type transactionsPerMinute struct {
	tpm int
}

func main() {
	data := ""
	transactions := 0
	startTime := time.Now()

	app := http.NewServeMux()
	dataApp := http.NewServeMux()

	app.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte("healthcheck"))
	})

	app.HandleFunc("/check-data", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte(data))
	})

	// Retrieve number of transactions per minute
	app.HandleFunc("/tpm", func(w http.ResponseWriter, _ *http.Request) {
		// Calculate duration in minutes
		now := time.Now()
		duration := now.Sub(startTime)
		tpm := transactions / int(duration.Minutes())
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(transactionsPerMinute{tpm})
	})

	// Separate data app from management app to be path agnostic
	dataReceived := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		data = "success"
		transactions++

		// Built-in latency
		time.Sleep(15 * time.Millisecond)
		w.WriteHeader(http.StatusOK)
	})

	dataApp.Handle("/put-data*", dataReceived)
	dataApp.Handle("/trace/v1", dataReceived)
	dataApp.Handle("/metric/v1", dataReceived)

	go log.Fatal(http.ListenAndServeTLS(":443", "./certificates/ssl/certificate.crt", "./certificates/private.key", dataApp))

	log.Fatal(http.ListenAndServe(":8080", app))
}
