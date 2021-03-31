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
		case <-timer:
			return fmt.Errorf("wait timeout after %s", time.Now().Sub(start))
		}
	}
}
