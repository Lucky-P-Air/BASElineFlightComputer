package com.platypii.baseline.altimeter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Sanity checks for kalman filter
 */
public abstract class FilterTest {

    public abstract Filter getFilter();

    @Test
    public void initialSample() {
        Filter filter = getFilter();

        // Add first sample
        filter.init(10, 0);

        assertEquals(10, filter.x(), .1);
        assertEquals(0, filter.v(), .1);
    }

    @Test
    public void initialUpdate() {
        Filter filter = getFilter();

        // Add samples
        filter.init(10, 0);
        filter.update(20, 1);
        assertEquals(20, filter.x(), 10);
        assertEquals(10, filter.v(), 10);
    }

    @Test
    public void constantVelocity() {
        Filter filter = getFilter();

        // Add samples
        filter.init(10, 0);
        filter.update(20, 1);
        filter.update(30, 1);
        filter.update(40, 1);
        filter.update(50, 1);
        filter.update(60, 1);
        filter.update(70, 1);
        filter.update(80, 1);
        filter.update(90, 1);

        assertEquals(90, filter.x(), 10);
        assertEquals(10, filter.v(), 3);
    }

    @Test
    public void handleNaN() {
        Filter filter = getFilter();

        // Add samples
        filter.init(10, 0);
        filter.update(20, 1);
        filter.update(30, 1);
        filter.update(40, 1);
        filter.update(50, 1);
        filter.update(60, 1);
        filter.update(Double.NaN, 1);
        filter.update(80, 1);
        filter.update(90, 1);

        assertEquals(90, filter.x(), 15);
        assertEquals(10, filter.v(), 4);
    }

}
