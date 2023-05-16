# Introduction

This directory holds the performance tests for the Group by Trace and Tail Sampling processors.

We are scaling the configuration of the two components so that it can handle the maximum load applied during the load tests which is 5000 samples per second.

We are using `wait_duration` of 2s in the groupytrace procssor. That means that in this interval we would have 10000 samples buffered. We are doubling this value in the `num_traces` property to be on the safe side.

In the Tail sampling processor there is a single policy based on latency.
