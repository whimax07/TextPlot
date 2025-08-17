package com.github.whimax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Gatherers;

public class LinePlot {

    private static final int NUMERIC_LENGTH = 8;

    private final List<Double> xData;
    private final List<Double> yData;
    private final int width;
    private final int height;
    private final char upper;
    private final char lower;
    private final Function<List<Double>, Double> yAverager;



    public LinePlot(
            List<Double> xData,
            List<Double> yData,
            int width,
            int height,
            CharSet charSet,
            Function<List<Double>, Double> yAverager
    ) {
        this.xData = xData;
        this.yData = yData;
        this.width = width;
        this.height = height;
        this.upper = charSet.upper;
        this.lower = charSet.lower;
        this.yAverager = yAverager;
    }



    public String draw() {
        return lines().stream().reduce((a, b) -> a + "\n" + b).orElseThrow();
    }

    public List<String> lines() {
        // Round up.
        final int binSize = (xData.size() + width - 1) / width;
        final List<Double> averageDataX = windowAverage(binSize, xData, Averager.MEAN.averager);

        final List<Double> averageDataY = windowAverage(binSize, yData, yAverager);
        final double rangeLow = averageDataY.stream().min(Double::compareTo).orElseThrow();
        final double rangeHigh = averageDataY.stream().max(Double::compareTo).orElseThrow();
        final double yScale = (rangeHigh - rangeLow) / height;
        // plotData will be less than or equal to height.
        // res = (y - rangeLow) / yScale
        //     = (y - rangeLow) / ((rangeHigh - rangeLow) / height)
        //     = ((y - rangeLow) * height) / (rangeHigh - rangeLow)
        //    <= ((rangeHigh - rangeLow) * height) / (rangeHigh - rangeLow)
        //    <= height
        final List<Integer> plotData = averageDataY.stream()
                .map(y -> y - rangeLow)
                .map(y -> y / yScale)
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
        for (int i = 0; i <= height; i++) {
            final double y = rangeLow + (yScale * i);
            final String clampedString = clampString(y);
            ticksY.add(clampedString);
        }

        final int stretchFactor = width / averageDataX.size();

        final String ticksX = writeTicksX(averageDataX, stretchFactor);

        final ArrayList<String> lines = new ArrayList<>();
        for (int i = height; i >= 0; i--) {
            final StringBuilder line = new StringBuilder(" ".repeat(NUMERIC_LENGTH + width));

            final String yTick = ticksY.get(i);
            line.replace(NUMERIC_LENGTH - yTick.length(), NUMERIC_LENGTH, yTick);

            final ArrayList<Integer> lowerPoints = plotMap.get(i * 2);
            final ArrayList<Integer> upperPoints = plotMap.get((i * 2) + 1);
            if (upperPoints != null) for (int x : upperPoints) {
                for (int stretch = 0; stretch < stretchFactor; stretch++) {
                    line.setCharAt((x * stretchFactor) + stretch + NUMERIC_LENGTH, upper);
                }
            }
            if (lowerPoints != null) for (int x : lowerPoints) {
                for (int stretch = 0; stretch < stretchFactor; stretch++) {
                    line.setCharAt((x * stretchFactor) + stretch + NUMERIC_LENGTH, lower);
                }
            }

            lines.add(line.toString());
        }

        lines.add(ticksX);
        return lines;
    }

    protected String writeTicksX(List<Double> averageData, int stretch) {
        final int tickGap = NUMERIC_LENGTH + 2;
        // Ceiling division.
        final int tickPeriod = (tickGap + (stretch - 1)) / stretch;
        // Half of NUMERIC_LENGTH before and after the axis.
        final StringBuilder stringBuilder = new StringBuilder(" ".repeat(width + NUMERIC_LENGTH));

        for (int i = 0; i < averageData.size(); i++) {
            if (i % tickPeriod != 0) continue;

            final double val = averageData.get(i);
            final String clamped = clampString(val);
            // "i" is the centre point for the tick on the x-axis, we overflow the left side so that the label at 0 is
            // centred.
            final int startIndex = NUMERIC_LENGTH + (i * stretch);
            stringBuilder.replace(startIndex, startIndex + clamped.length(), clamped);
        }

        return stringBuilder.toString();
    }



    private static List<Double> windowAverage(
            int windowSize, List<Double> data, Function<List<Double>, Double> yAverager
    ) {
        return data.stream()
                .gather(Gatherers.windowFixed(windowSize))
                .map(yAverager)
                .toList();
    }

    private static String clampString(double toClamp) {
        final String asString = Double.toString(toClamp);
        if (asString.length() <= NUMERIC_LENGTH) return asString;
        return asString.substring(0, NUMERIC_LENGTH - 3) + "...";
    }



    public enum CharSet {
        ASIC('-', '_'),
        BLOCK('▀', '▄'),
        LINE('—', '_')
        ;

        final char upper;

        final char lower;
        CharSet(char upper, char lower) {
            this.upper = upper;
            this.lower = lower;
        }

    }

    public enum Averager {
        MEAN(l -> l.stream().reduce(0.0, Double::sum) / l.size()),
        HOLD_HIGH(l -> l.stream().max(Comparator.naturalOrder()).orElseThrow()),
        HOLD_LOW(l -> l.stream().min(Comparator.naturalOrder()).orElseThrow()),
        FIRST(List::getFirst),
        LAST(List::getLast)
        ;

        final Function<List<Double>, Double> averager;

        Averager(Function<List<Double>, Double> averager) {
            this.averager = averager;
        }

    }

}
