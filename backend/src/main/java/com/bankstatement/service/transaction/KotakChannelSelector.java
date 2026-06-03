package com.bankstatement.service.transaction;

import com.bankstatement.entity.TransactionSettings;

import java.time.YearMonth;
import java.util.Random;
import java.util.Set;

/** Weighted channel mix: UPI ~80%, IMPS ~10%, NEFT ~8%, INT ~2% (once per month). */
final class KotakChannelSelector {

    private KotakChannelSelector() {}

    static KotakChannel pick(Random random, ActivityFlowPlanner.Direction direction,
                            TransactionSettings settings, YearMonth month,
                            Set<YearMonth> interestMonths) {
        if (direction == ActivityFlowPlanner.Direction.DEBIT) {
            return KotakChannel.UPI_DEBIT;
        }

        int roll = random.nextInt(100);
        if (roll < 80) {
            int variant = random.nextInt(100);
            if (variant < 40) {
                return KotakChannel.UPI_CREDIT;
            }
            if (variant < 70) {
                return KotakChannel.UPI_CR;
            }
            return KotakChannel.UPI_REV;
        }
        if (roll < 90) {
            return KotakChannel.IMPS;
        }
        return KotakChannel.NEFT;
    }
}
