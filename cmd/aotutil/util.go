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
	"fmt"
	"time"
)

type WaitAction int

const (
	WaitContinue WaitAction = iota + 1
	WaitDone
)

// Wait run f once then start polling based on interval.
// It stops when f return any error. It does NOT have retryable error logic.
// i.e. it checks error before checking action.  If you need to deal with retryable error,
// you should handle it inside f and return (WaitContinue, nil).
func Wait(interval, timeout time.Duration, f func() (WaitAction, error)) error {
	// Run f once
	act, err := f()
	if err != nil {
		return err
	}
	if act == WaitDone {
		return nil
	}

	// Poll
	start := time.Now()
	timer := time.After(timeout)
	ticker := time.NewTicker(waitInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			act, err := f()
			if err != nil {
				return err
			}
			if act == WaitDone {
				return nil
			}
			// Keep polling
		case <-timer:
			return fmt.Errorf("wait timeout after %s", time.Now().Sub(start))
		}
	}
}
