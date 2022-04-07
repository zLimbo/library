package bftsmart.benchmark;

import bftsmart.tests.recovery.Operation;
import bftsmart.tom.ServiceProxy;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author robin
 */
public class ThroughputLatencyClient {
	private static int initialClientId;
	private static byte[] data;
	private static byte[] serializedReadRequest;
	private static byte[] serializedWriteRequest;

	private static AtomicLong latencySum = new AtomicLong();
	private static AtomicLong countOfRequest = new AtomicLong();
	private static AtomicInteger numOfClientRunning = new AtomicInteger();
	static long start;
	static long interval;

	public static void main(String[] args) throws InterruptedException {
		if (args.length < 6) {
			System.out.println("USAGE: bftsmart.benchmark.ThroughputLatencyClient <initial client id> " +
					"<num clients> <number of operations per client> <request size> <isWrite?> <measurement leader?>");
			System.exit(-1);
		}

		initialClientId = Integer.parseInt(args[0]);
		int numClients = Integer.parseInt(args[1]);
		int numOperationsPerClient = Integer.parseInt(args[2]);
		int requestSize = Integer.parseInt(args[3]);
		boolean isWrite = Boolean.parseBoolean(args[4]);
		boolean measurementLeader = Boolean.parseBoolean(args[5]);

		int bootDelay = 10;
		if (args.length > 6) {
			bootDelay = Integer.parseInt(args[6]);
		}
		interval = 0;
		if (args.length > 7) {
			interval = Long.parseLong(args[7]);
		}

		System.out.println("initialClientId: " + initialClientId +
				"\nnumClients: " + numClients +
				"\nnumOperationsPerClient: " + numOperationsPerClient +
				"\nrequestSize: " + requestSize +
				"\nisWrite: " + isWrite +
				"\nmeasurementLeader: " + measurementLeader +
				"\nbootDelay: " + bootDelay +
				"\ninterval: " + interval);

		CountDownLatch latch = new CountDownLatch(numClients);
		Client[] clients = new Client[numClients];
		data = new byte[requestSize];
		for (int i = 0; i < requestSize; i++) {
			data[i] = (byte) i;
		}
		ByteBuffer writeBuffer = ByteBuffer.allocate(1 + Integer.BYTES + requestSize);
		writeBuffer.put((byte) Operation.PUT.ordinal());
		writeBuffer.putInt(requestSize);
		writeBuffer.put(data);
		serializedWriteRequest = writeBuffer.array();

		ByteBuffer readBuffer = ByteBuffer.allocate(1);
		readBuffer.put((byte) Operation.GET.ordinal());
		serializedReadRequest = readBuffer.array();

		// new Thread(() -> {
		// try {
		// latch.await();
		// System.out.println("Executing experiment");
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }).start();

		System.out.println("Executing experiment");
		start = System.nanoTime();
		for (int i = 0; i < numClients; i++) {
			clients[i] = new Client(initialClientId + i,
					numOperationsPerClient, isWrite, measurementLeader, latch);
			clients[i].start();
			Thread.sleep(bootDelay);
		}

		for (Client client : clients) {
			client.join();
		}
		double avgLatency = (double) latencySum.get() / countOfRequest.get() / 1e6;
		long take = System.nanoTime() - start;
		double tps = (double) countOfRequest.get() * 1e9 / take;

		System.out.println("tx_num: " + countOfRequest.get());
		System.out.printf("take: %.6f(s)\n", (double) take / 1e9);
		System.out.printf("tps: %.6f, latency: %.6f(ms)\n", tps, avgLatency);
		System.out.printf("%d\t%d\t%.2f\n", numClients, (int) (tps * requestSize / 100), avgLatency);
	}

	private static class Client extends Thread {
		private final int clientId;
		private final int numOperations;
		private final boolean isWrite;
		private final ServiceProxy proxy;
		private final CountDownLatch latch;
		private final boolean measurementLeader;

		public Client(int clientId, int numOperations, boolean isWrite, boolean measurementLeader,
				CountDownLatch latch) {
			this.clientId = clientId;
			this.numOperations = numOperations;
			this.isWrite = isWrite;
			this.measurementLeader = measurementLeader;
			this.proxy = new ServiceProxy(clientId);
			this.latch = latch;
			this.proxy.setInvokeTimeout(40000);
		}

		@Override
		public void run() {
			try {
				// latch.countDown();
				if (initialClientId == clientId) {
					proxy.invokeOrdered(serializedWriteRequest);
				}
				System.out.println(clientId + " start");
				numOfClientRunning.incrementAndGet();
				for (int i = 0; i < numOperations; i++) {
					long t1, t2, latency;
					byte[] response;
					t1 = System.nanoTime();
					if (isWrite) {
						response = proxy.invokeOrdered(serializedWriteRequest);
					} else {
						response = proxy.invokeOrdered(serializedReadRequest);
					}
					t2 = System.nanoTime();
					latency = t2 - t1;
					latencySum.addAndGet((long) latency);
					countOfRequest.incrementAndGet();
					if (!isWrite && !Arrays.equals(data, response)) {
						throw new IllegalStateException("The response is wrong");
					}
					// if (initialClientId == clientId && measurementLeader) {
					if (measurementLeader) {

						double avgLatency = (double) latencySum.get() / countOfRequest.get() / 1e6;
						long take = System.nanoTime() - start;
						double tps = (double) countOfRequest.get() * 1e9 / take;

						System.out.printf(
								"client_id: %d, op_seq: %d, latency: %.2f | client_num: %d, tx_num: %d, tps: %.6f, avg_latency: %.6f\n",
								clientId, i, (double) latency / 1e6, numOfClientRunning.get(), countOfRequest.get(),
								tps, avgLatency);

					}
					// try {
					// Thread.sleep(interval);
					// } catch (InterruptedException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
				}
				System.out.println(clientId + " finished");
				numOfClientRunning.decrementAndGet();
			} finally {
				proxy.close();
			}
		}
	}
}
