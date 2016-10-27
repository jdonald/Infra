package jiyuiydi.util;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class ExceptionalLatchTest {

	@Test
	public void testStandardLibraryLatchUsage() throws InterruptedException {
		int n = 30;

		CountDownLatch cdl = new CountDownLatch(n);
		for (int i = 0; i < n; ++i)
			new Thread(new Runnable() {
				public void run() {
					try { Thread.sleep((int) (Math.random() * 200)); } catch (Exception e) {}
					//int a = 4/0; // cause a RuntimeException
					cdl.countDown();
				}
			}).start();

		cdl.await();
		assertEquals(0, cdl.getCount());
	}

	@Test
	public void testDecrementUsage() {
		int n = 30;

		ExceptionalLatch cl = new ExceptionalLatch(n);
		for (int i = 0; i < n; ++i)
			new Thread(new Runnable() {
				public void run() {
					try { Thread.sleep((int) (Math.random() * 200)); } catch (Exception e) {}
					cl.decrement();
				}
			}).start();

		try {
			cl.await();
		} catch (InterruptedException e) {
			assertFalse(true);
		}
		assertEquals(0, cl.getCount());
	}

	@Test
	public void testIncrementUsage() {
		int n = 25, re = 5;

		ExceptionalLatch cl = new ExceptionalLatch();

		for (int i = 0; i < n; ++i)
			cl.increment(new Runnable() {
				public void run() {
					try { Thread.sleep((int) (Math.random() * 200)); } catch (Exception e) {}
				}
			});
		for (int i = 0; i < re; ++i)
			cl.increment(new Runnable() {
				public void run() {
					try { Thread.sleep((int) (Math.random() * 200)); } catch (Exception e) {}
					throw new RuntimeException(); // cause a RuntimeException
				}
			});

		boolean caught = false;
		try {
			cl.await();
		} catch(InterruptedException e) {
			caught = true;
		}
		assertTrue(caught);
		assertEquals(re, cl.getExceptions().size());
		assertEquals(re, cl.getCount());
		assertEquals(0, cl.getCount() - cl.getExceptions().size());
	}

}
