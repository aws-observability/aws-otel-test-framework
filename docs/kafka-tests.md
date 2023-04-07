## Introduction

This document describes how the Kafka receiver and exporter are tested. We need
to pay special attention to the tests of these components. The test of the Kafka
receiver and exporter is similar to other components with exception that we are
combining the tests of the exporter and receiver in the same test case.
The reason we need to use this approach is because the Kafka exporter and receiver
only operate on serialized forms of the telemetry signals. Since we don’t want to test if we can properly
serialize/deserialize OTLP signals, we opted to test these components wired together.

When a testcase involving these components is run, data will be exported to Kafka
and back again to the collector. The validator will then verify that the data is
correct after this round trip is completed.

All the tests of these components require two pipelines:

* Pipeline 1: Signal receiver -> processor -> Kafka exporter -> MSK Cluster (permanent infrastructure)
* Pipeline 2: Kafka receiver -> processor -> Signal exporter (Exports to the backend of choice).
