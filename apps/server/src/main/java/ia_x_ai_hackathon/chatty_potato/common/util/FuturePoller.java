package ia_x_ai_hackathon.chatty_potato.common.util;


import ia_x_ai_hackathon.chatty_potato.rag.exception.TimeoutException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class FuturePoller {
	private FuturePoller() {}

	public static <T> T awaitWithDeadline(
			CompletableFuture<T> future,
			long waitMillis,
			long stepMillis,
			Supplier<Boolean> cancelOrErrorProbe
	) throws InterruptedException, ExecutionException, TimeoutException {

		if (waitMillis <= 0) throw new TimeoutException("waitMillis <= 0");

		final long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
		final long step = Math.max(10, stepMillis);

		T v = future.getNow(null);
		if (v != null) return v;
		if (future.isCompletedExceptionally()) return future.get();

		while (true) {
			if (cancelOrErrorProbe != null && Boolean.TRUE.equals(cancelOrErrorProbe.get())) {
				if (!future.isDone()) throw new ExecutionException(new IllegalStateException("Cancelled or error signaled"));
			}

			long leftNanos = deadlineNanos - System.nanoTime();
			if (leftNanos <= 0) throw new TimeoutException("Prompt not ready within deadline");

			long thisWait = Math.min(step, Math.max(1, TimeUnit.NANOSECONDS.toMillis(leftNanos)));

			try {
				return future.get(thisWait, TimeUnit.MILLISECONDS);
			} catch (TimeoutException te) {
				if (future.isCompletedExceptionally()) return future.get();
			} catch (java.util.concurrent.TimeoutException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
