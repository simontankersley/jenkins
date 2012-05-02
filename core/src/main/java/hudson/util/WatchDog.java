package hudson.util;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * A WatchDog is an object that will interrupt a thread after a specified time
 * until stop is called.
 * 
 * It exists to interrupt a thread that is taking too long to complete an
 * operation.
 * 
 * To use wrap the operation code in this code block
 * 
 * <pre>
 * long timeout = // the timeout in millis
 * WatchDog watchDog = new WatchDog(Thread.currentThread(), timeout);
 * watchDog.start();
 * try {
 *    // perform operation
 * } catch (InterruptedException e) {
 *    watchDog.stop();
 * }
 * </pre>
 * 
 * Note that it important to catch the exception that is caused by the
 * interrupt. This exception might not be an InterruptedException. For example
 * when interrupting IO operations an {@link InterruptedIOException} is thrown (which is
 * a subclass of {@link IOException}).
 */
public class WatchDog {

	private Thread watched;
	private int timeout;

	private Thread watchDogThread;
	private long startTime;
	private volatile boolean stop;
	private volatile boolean stopped;

	public WatchDog(Thread watched, int timeout) {
		this.watched = watched;
		this.timeout = timeout;
	}

	/**
	 * Start the watch dog thread that will interrupt the watched thread after
	 * the specified timeout unless {@link WatchDog#stop} is called first.
	 */
	public void start() {
		watchDogThread = new Thread(new Runnable() {
			public void run() {
				startTime = System.currentTimeMillis();
				while (!stop) {
					if (System.currentTimeMillis() > startTime + timeout) {
						break;
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// ignore
						}
					}
				}
				if (!stop) {
					watched.interrupt();
				}
				stopped = true;
			}
		});
		watchDogThread.start();
	}

	/**
	 * Tell the watch dog that the operation is complete and it should not
	 * interrupt the watched thread.
	 * 
	 * The watch dog will not interrupt the thread after this method has been
	 * called.
	 */
	public void stop() {
		stop = true;
		while (!stopped) {
			watchDogThread.interrupt();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore the interrupt
			}
		}
		// clear the interrupt flag in case the watch dog thread still managed
		// to interrupt this thread before setting stopped to true
		Thread.currentThread().interrupted();
	}

}
