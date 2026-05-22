#!/bin/bash
# Stops all port-forward processes and keepalive monitor.
kill $(cat /tmp/pf_keepalive.pid 2>/dev/null) 2>/dev/null || true
kill $(cat /tmp/pf_mocked.pid 2>/dev/null) 2>/dev/null || true
kill $(cat /tmp/pf_sample.pid 2>/dev/null) 2>/dev/null || true
rm -f /tmp/pf_keepalive.pid /tmp/pf_mocked.pid /tmp/pf_sample.pid /tmp/pf_keepalive.log /tmp/pf_mocked.log /tmp/pf_sample.log
