package com.github.whimax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Gatherers;

public class LinePlot {

    private static final char UPPER_HALF = '▀';
    private static final char LOWER_HALF = '▄';
    private static final char UPPER_ASIC = '-';
    private static final char LOWER_ASIC = '_';
    private static final int NUMERIC_LENGTH = 8;

    private final List<Double> xData;
    private final List<Double> yData;
    private final int width;
    private final int height;
    private final char upper;
    private final char lower;



    public LinePlot(List<Double> xData, List<Double> yData, int width, int height, boolean utf8) {
        this.xData = xData;
        this.yData = yData;
        this.width = width;
        this.height = height;
        this.upper = utf8 ? UPPER_HALF : UPPER_ASIC;
        this.lower = utf8 ? LOWER_HALF : LOWER_ASIC;
    }



    public String draw() {
        return lines().stream().reduce((a, b) -> a + "\n" + b).orElseThrow();
    }

    public List<String> lines() {
        // Round up.
        final int binSize = (xData.size() + width - 1) / width;
        final List<Double> averageDataX = windowAverage(binSize, xData);

        final List<Double> averageDataY = windowAverage(binSize, yData);
        final double rangeLow = averageDataY.stream().min(Double::compareTo).orElseThrow();
        final double rangeHigh = averageDataY.stream().max(Double::compareTo).orElseThrow();
        final double yScale = (rangeHigh - rangeLow) / (height - 1);
        final List<Integer> plotData = averageDataY.stream()
                .map(y -> y - rangeLow)
                .map(y -> y * yScale)
                // We have 2 positions per cell.
                .map(y -> y * 2)
                .map(Math::round)
                .map(Long::intValue)
                .toList();

        final HashMap<Integer, ArrayList<Integer>> plotMap = new HashMap<>();
        for (int i = 0; i < plotData.size(); i++) {
            final int y = plotData.get(i);
            plotMap.putIfAbsent(y, new ArrayList<>());
            final ArrayList<Integer> indexes = plotMap.get(y);
            indexes.add(i);
        }

        final List<String> ticksY = new ArrayList<>();
        for (int i = height - 1; i >= 0; i--) {
            final double y = rangeLow + (yScale * i);
            final String clampedString = clampString(y);
            ticksY.add(clampedString);
        }

        final String ticksX = writeTicksX(averageDataX);

        final ArrayList<String> lines = new ArrayList<>();
        for (int i = height - 1; i >= 0; i--) {
            final StringBuilder line = new StringBuilder(" ".repeat(NUMERIC_LENGTH + width));

            final String yTick = ticksY.get(i);
            line.replace(NUMERIC_LENGTH - yTick.length(), NUMERIC_LENGTH, yTick);

            for (int x : plotMap.get((i * 2) + 1)) {
                line.setCharAt(x, upper);
            }
            for (int x : plotMap.get(i * 2)) {
                line.setCharAt(x, lower);
            }

            lines.add(line.toString());
        }

        lines.add(ticksX);
        return lines;
    }

    protected String writeTicksX(List<Double> averageData) {
        final int tickGap = NUMERIC_LENGTH + 2;
        // Half of NUMERIC_LENGTH before and after the axis.
        final StringBuilder stringBuilder = new StringBuilder(" ".repeat(width + NUMERIC_LENGTH));
        final int leftOffset = NUMERIC_LENGTH / 2;

        for (int i = 0; i < averageData.size(); i++) {
            if (i % tickGap != 0) continue;

            final double val = averageData.get(i);
            final String clamped = clampString(val);
            // "i" is the centre point for the tick on the x-axis, we overflow the left side so that the label at 0 is
            // centred.
            final int startIndex = leftOffset + i - (clamped.length() / 2);
            stringBuilder.replace(startIndex, startIndex + clamped.length(), clamped);
        }

        return stringBuilder.toString();
    }



    private static List<Double> windowAverage(int windowSize, List<Double> data) {
        return data.stream()
                .gather(Gatherers.windowFixed(windowSize))
                .map(window -> {
                    final double sum = window.stream().mapToDouble(Double::doubleValue).sum();
                    return sum / window.size();
                })
                .toList();
    }

    private static String clampString(double toClamp) {
        final String asString = Double.toString(toClamp);
        if (asString.length() <= NUMERIC_LENGTH) return asString;
        return asString.substring(0, NUMERIC_LENGTH - 3) + "...";
    }

}
