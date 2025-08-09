package com.github.whimax;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinePlotTest {

    @Test
    public void basicPlot() {
        final List<Double> xData = List.of(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0);
        final List<Double> yData = List.of(21.0, 23.0, 15.0, 14.0, 24.0, 25.0, 25.0, 26.0, 27.0, 27.0, 28.0, 27.0);
        final int width = 30;
        final int height = 7;
        final String graph = new LinePlot(xData, yData, width, height, LinePlot.CharSet.BLOCK).draw();
        System.out.println(graph);
    }

}