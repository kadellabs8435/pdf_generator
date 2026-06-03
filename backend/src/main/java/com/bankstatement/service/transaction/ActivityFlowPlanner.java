package com.bankstatement.service.transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Same-day bursts and natural credit/debit alternation for FinBox high-value banks. */
final class ActivityFlowPlanner {

    private ActivityFlowPlanner() {}

    enum Direction {
        CREDIT, DEBIT
    }

    /**
     * Patterns like: credit, debit, credit, credit, debit.
     */
    static List<Direction> planDirections(int count, Random random) {
        return planDirections(count, random, false);
    }

    /**
     * Kotak: natural balance oscillation (up/down), not a monotonic upward curve.
     */
    static List<Direction> planDirectionsKotak(int count, Random random) {
        return planDirections(count, random, true);
    }

    private static List<Direction> planDirections(int count, Random random, boolean kotakOscillation) {
        List<Direction> flow = new ArrayList<>(count);
        Direction next = Direction.CREDIT;
        for (int i = 0; i < count; i++) {
            flow.add(next);
            if (kotakOscillation) {
                if (next == Direction.CREDIT) {
                    next = random.nextInt(100) < 72 ? Direction.DEBIT : Direction.CREDIT;
                } else {
                    next = random.nextInt(100) < 58 ? Direction.CREDIT : Direction.DEBIT;
                }
            } else if (next == Direction.CREDIT) {
                next = random.nextInt(100) < 38 ? Direction.CREDIT : Direction.DEBIT;
            } else {
                next = Direction.CREDIT;
            }
        }
        return flow;
    }

    /**
     * Assigns transaction dates with 2–5 transactions on burst days.
     */
    static List<LocalDate> planDates(LocalDate from, LocalDate to, int count, Random random) {
        long span = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        int maxDay = (int) Math.max(0, span);
        List<LocalDate> dates = new ArrayList<>(count);
        LocalDate burstDay = null;
        int burstRemaining = 0;

        for (int i = 0; i < count; i++) {
            if (burstRemaining <= 0 && random.nextInt(100) < 40) {
                burstDay = from.plusDays(maxDay == 0 ? 0 : random.nextInt(maxDay + 1));
                burstRemaining = 2 + random.nextInt(4);
            }
            if (burstRemaining > 0) {
                dates.add(burstDay);
                burstRemaining--;
            } else {
                dates.add(from.plusDays(maxDay == 0 ? 0 : random.nextInt(maxDay + 1)));
            }
        }
        return dates;
    }
}
