package main

import (
	"encoding/json"
	"net/http"
)

type MetricResponse struct {
	Labels map[string]string `json:"metric"`
	Value  []string          `json:"value"`
}

func retrieveExpectedMetricsHelper(w http.ResponseWriter, r *http.Request, metrics *[]MetricResponse) {
	switch r.Method {
	case "GET":
		response, _ := json.Marshal(*metrics)
		w.Header().Set("Content-Type", "application/json")
		w.Write(response)
	}
}
