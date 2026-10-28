package com.agristep.ui;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs slow work (database, internet) off the screen thread so the window never freezes,
 * then hands the result back on the screen thread.
 */
final class Tasks {
    private Tasks() {}

    /** Tests switch this on to run everything immediately on the calling thread. */
    static volatile boolean synchronous = false;

    static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        if (synchronous) {
            T result;
            try {
                result = work.call();
            } catch (Throwable t) {
                onFailure.accept(t);
                return;
            }
            onSuccess.accept(result);
            return;
        }
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }

            @Override protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onFailure.accept(e.getCause() == null ? e : e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Runs {@code work} on the screen thread (used by code that may be called from anywhere). */
    static void later(Runnable work) {
        if (SwingUtilities.isEventDispatchThread()) work.run();
        else SwingUtilities.invokeLater(work);
    }

    static String messageOf(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String message = t.getMessage() != null ? t.getMessage() : root.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : I18n.t(message);
    }
}
