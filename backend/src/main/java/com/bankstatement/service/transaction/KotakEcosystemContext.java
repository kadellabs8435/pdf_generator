package com.bankstatement.service.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Persistent diverse Indian names for one Kotak statement generation.
 */
final class KotakEcosystemContext {

    private final Random random;
    private final List<String> names;
    private int nameCursor;

    KotakEcosystemContext(Random random, String accountNumber) {
        this.random = random;
        this.names = IndianNamesPool.shuffled(random);
        this.nameCursor = random.nextInt(names.size());
    }

    String nextName() {
        String name = names.get(nameCursor);
        nameCursor = (nameCursor + 1) % names.size();
        return IndianNamesPool.uppercaseForNarration(name);
    }

    String pickName() {
        return IndianNamesPool.uppercaseForNarration(names.get(random.nextInt(names.size())));
    }

    List<String> names() {
        return Collections.unmodifiableList(names);
    }

    /** SBI high-value path reuses the same name pool. */
    String nextCounterparty() {
        return nextName();
    }

    String pickCounterparty() {
        return pickName();
    }

    String nextMerchant() {
        return pickName();
    }

    String pickMerchant() {
        return pickName();
    }
}
