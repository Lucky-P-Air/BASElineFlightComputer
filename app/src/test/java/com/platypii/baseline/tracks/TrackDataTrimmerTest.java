package com.platypii.baseline.tracks;

import com.platypii.baseline.measurements.MLocation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrackDataTrimmerTest {

    @Test
    public void autoTrim() {
        List<MLocation> untrimmed = new ArrayList<>();
        addPoints(untrimmed, 0, 100);
        addPoints(untrimmed, -20, 100);
        addPoints(untrimmed, 0, 100);
        List<MLocation> trimmed = TrackDataTrimmer.autoTrim(untrimmed);
        assertEquals(200, trimmed.size());
    }

    private void addPoints(List<MLocation> list, double climb, int count) {
        for (int i = 0; i < count; i++) {
            list.add(new MLocation(0L, 0.0, 1.0, 2.0, climb, 0.0, 0.0, 0f, 0f, 0f, 0f, 0, 0));
        }
    }

}