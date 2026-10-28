package com.agristep.repository;

import java.util.function.Supplier;

/** Runs several database operations as one all-or-nothing unit. */
public interface Transactions {
    <T> T run(Supplier<T> work);

    default void run(Runnable work) {
        run(() -> { work.run(); return null; });
    }

    /** Runs the work directly with no transaction. Used by unit tests. */
    Transactions NONE = new Transactions() {
        @Override public <T> T run(Supplier<T> work) { return work.get(); }
    };
}
