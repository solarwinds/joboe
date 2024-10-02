package com.tracelytics.profiler;

import com.tracelytics.profiler.Profiler.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CircuitBreakerTest {
    @Test
    public void testPause() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(100, 3);
        
        
        /**
         * Copied from description on CircuitBreaker.pauseMethod
         * 
         * This might mutate the current circuit breaker states, depending on the current state and the duration parameters: 
         * 
         * At first the circuit breaker starts with a "Normal" state
         * When there are n (defined by `countThreshold`) consecutive `getPause` calls with param `duration` above the `durationThreshold`, the circuit breaker will go into the "Break" state
         * "Break" state will be transitioned into a "Restored but broken recently" state when there's a new `getPause` call 
         * "Restored but broken recently" state will be transitioned to "Normal" state if there are n consecutive `getPause` calls with param `duration` below or equal to the `durationThreshold`
         * 
         * And below are the behaviors of this method in various states/transitions:
         *  
         * When transition to "Normal" state, `nextPause` is set to INITIAL_CIRCUIT_BREAKER_PAUSE
         * When in "Normal" state, `getPause` returns 0
         * When transition to "Break" state, `getPause` returns `nextPause` then `nextPause` is multiplied by `PAUSE_MULTIPLIER`
         * When transition to or in "Restored but broken recently" state, `getPause` returns 0 */
        
        assertEquals(0, circuitBreaker.getPause(0)); //no break
        assertEquals(0, circuitBreaker.getPause(100)); //no break, still within threshold
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 1 consecutive occurrence 
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 2 consecutive occurrences
        assertEquals(CircuitBreaker.INITIAL_CIRCUIT_BREAKER_PAUSE, circuitBreaker.getPause(101)); //break, above threshold and 3 consecutive occurrences
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold and should transition into "Restored but broken recently" state
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 2 consecutive occurrences
        assertEquals((int) (CircuitBreaker.INITIAL_CIRCUIT_BREAKER_PAUSE * CircuitBreaker.PAUSE_MULTIPLIER), circuitBreaker.getPause(101)); //break again with increased pause, above threshold and 3 consecutive occurrences
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold and should transition into "Restored but broken recently" state
        assertEquals(0, circuitBreaker.getPause(0)); //no break - not resetting nextPause yet, below threshold but only 1 consecutive occurrence
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 1 consecutive occurrence
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 2 consecutive occurrence
        assertEquals((int) (CircuitBreaker.INITIAL_CIRCUIT_BREAKER_PAUSE * CircuitBreaker.PAUSE_MULTIPLIER * CircuitBreaker.PAUSE_MULTIPLIER), circuitBreaker.getPause(101)); //break again with increased pause, above threshold and 3 consecutive occurrences
        assertEquals(0, circuitBreaker.getPause(0)); //no break - not resetting nextPause yet, below threshold but only 1 consecutive occurrence
        assertEquals(0, circuitBreaker.getPause(0)); //no break - not resetting nextPause yet, below threshold but only 2 consecutive occurrences
        assertEquals(0, circuitBreaker.getPause(0)); //no break - resetting nextPause, below threshold and 3 consecutive occurrences
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 1 consecutive occurrence
        assertEquals(0, circuitBreaker.getPause(101)); //no break, above threshold but only 2 consecutive occurrence
        assertEquals(CircuitBreaker.INITIAL_CIRCUIT_BREAKER_PAUSE, circuitBreaker.getPause(101)); //break but only pause for INITIAL_CIRCUIT_BREAKER_PAUSE, above threshold and 3 consecutive occurrences
    }
    
        
}
