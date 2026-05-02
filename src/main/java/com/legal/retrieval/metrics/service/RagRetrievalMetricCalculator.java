package com.legal.retrieval.metrics.service;

public final class RagRetrievalMetricCalculator {

    private RagRetrievalMetricCalculator() {
    }

    public static double recall(int truePositive, int falseNegative) {
        return ratio(truePositive, truePositive + falseNegative);
    }

    public static double precision(int truePositive, int falsePositive) {
        return ratio(truePositive, truePositive + falsePositive);
    }

    private static double ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return (double) numerator / denominator;
    }
}
