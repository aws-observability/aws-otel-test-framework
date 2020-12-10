package main

import (
	"fmt"
	"math"
	"math/rand"
	"net/http"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	"log"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	mtx          sync.Mutex
	testingID    string
	mc           = newMetricCollector()
	promRegistry = prometheus.NewRegistry() // local Registry so we don't get Go metrics, etc.
)

type metricBatch struct {
	counter   prometheus.Counter
	gauge     prometheus.Gauge
	histogram prometheus.Histogram
	summary   prometheus.Summary
}

func main() {
	testingID = os.Getenv("INSTANCE_ID")
	address := loadAddressFromEnv()
	metricCount := loadMetricCountFromEnv()

	rand.Seed(time.Now().Unix())

	registerMetrics(metricCount)
	go updateMetrics()

	http.HandleFunc("/", healthCheckHandler)
	http.Handle("/metrics", promhttp.HandlerFor(promRegistry, promhttp.HandlerOpts{}))
	http.HandleFunc("/expected_metrics", retrieveExpectedMetrics)

	log.Fatal(http.ListenAndServe(address, nil))
}

func loadAddressFromEnv() string {
	address, ok := os.LookupEnv("LISTEN_ADDRESS")
	if !ok {
		fmt.Println("Invalid address entered, serving on 0.0.0.0:8080")
		return "0.0.0.0:8080"
	}
	address = strings.TrimSpace(address)
	fmt.Println("Serving on address: " + address)
	return address
}

func loadMetricCountFromEnv() int {
	metricCount, ok := os.LookupEnv("METRICS_LOAD")
	if !ok {
		metricCount = "1"
	}
	numMetrics, err := strconv.Atoi(metricCount)
	if err != nil {
		log.Fatal(err)
	}
	return numMetrics
}

func updateMetrics() {
	for {
		time.Sleep(time.Second * 30)
		mtx.Lock()
		mc.timestamp = float64(time.Now().UnixNano()) / 1000000000
		for idx := 0; idx < mc.metricCount; idx++ {
			mc.counters[idx].Add(rand.Float64())
			mc.gauges[idx].Add(rand.Float64())
			lowerBound := math.Mod(rand.Float64(), 1)
			increment := math.Mod(rand.Float64(), 0.05)
			for i := lowerBound; i < 1; i += increment {
				mc.histograms[idx].Observe(i)
				mc.summarys[idx].Observe(i)
			}
		}
		mtx.Unlock()
	}
}

func retrieveExpectedMetrics(w http.ResponseWriter, r *http.Request) {
	mtx.Lock()
	defer mtx.Unlock()

	metricsResponse := mc.convertMetricsToExportedMetrics()
	retrieveExpectedMetricsHelper(w, r, metricsResponse)
}

func registerMetrics(metricCount int) {
	mc.metricCount = metricCount
	for idx := 0; idx < metricCount; idx++ {
		namespace := "test" + testingID
		counter := prometheus.NewCounter(
			prometheus.CounterOpts{
				Namespace: namespace,
				Name:      fmt.Sprintf("counter%v", idx),
				Help:      "This is my counter",
				// labels can be added like this
				// ConstLabels: prometheus.Labels{
				// 	"label1": "val1",
				// },
			})
		gauge := prometheus.NewGauge(
			prometheus.GaugeOpts{
				Namespace: namespace,
				Name:      fmt.Sprintf("gauge%v", idx),
				Help:      "This is my gauge",
			})
		histogram := prometheus.NewHistogram(
			prometheus.HistogramOpts{
				Namespace: namespace,
				Name:      fmt.Sprintf("histogram%v", idx),
				Help:      "This is my histogram",
				Buckets:   []float64{0.005, 0.1, 1},
			})
		summary := prometheus.NewSummary(
			prometheus.SummaryOpts{
				Namespace: namespace,
				Name:      fmt.Sprintf("summary%v", idx),
				Help:      "This is my summary",
				Objectives: map[float64]float64{
					0.1:  0.5,
					0.5:  0.5,
					0.99: 0.5,
				},
			})

		promRegistry.MustRegister(counter)
		promRegistry.MustRegister(gauge)
		promRegistry.MustRegister(histogram)
		promRegistry.MustRegister(summary)

		mc.counters = append(mc.counters, counter)
		mc.gauges = append(mc.gauges, gauge)
		mc.histograms = append(mc.histograms, histogram)
		mc.summarys = append(mc.summarys, summary)
	}
}

func healthCheckHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "healthy")
}
