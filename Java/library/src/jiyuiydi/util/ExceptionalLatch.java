package jiyuiydi.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map.Entry;

public class ExceptionalLatch {

	private int remaining;
	private final HashMap<Thread, Throwable> exceptions = new HashMap<>();

	public ExceptionalLatch() {}

	public ExceptionalLatch(int count) { remaining = count; }

	public int getCount() { return remaining; }

	public HashMap<Thread, Throwable> getExceptions() { return exceptions; }

	public String exceptionsToString() {
		StringBuilder sb = new StringBuilder();
		for(Entry<Thread, Throwable> tt : exceptions.entrySet()) {
			sb.append(tt.getKey().toString());
			sb.append(" : ");
			sb.append(tt.getValue().toString());
			sb.append('\n');
		}
		return sb.toString();
	}

	public synchronized void increment(Runnable r) {
		++remaining;
		Thread t = new Thread(new Runnable() { // a wrapper to decrement after the Runnable completes
			public void run() {
				r.run();
				decrement();
			}
		});
		t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() { // detect premature termination
			public void uncaughtException(Thread t, Throwable e) {
				synchronized(ExceptionalLatch.this) {
					exceptions.put(t, e);
					ExceptionalLatch.this.notifyAll();
				}
			}
		});
		t.start();
	}

	public synchronized void decrement() {
		--remaining;
		if(allDone()) notifyAll();
	}

	public synchronized void await() throws InterruptedException {
		while(!allDone()) wait();
		if(!exceptions.isEmpty()) throw new InterruptedException();
	}

	private boolean allDone() {
		return remaining - exceptions.size() == 0;
	}

}
