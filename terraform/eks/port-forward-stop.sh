#!/bin/bash
# Stops port-forward processes. Called by terraform on destroy.
kill $(cat /tmp/pf_mocked.pid 2>/dev/null) $(cat /tmp/pf_sample.pid 2>/dev/null) 2>/dev/null || true
