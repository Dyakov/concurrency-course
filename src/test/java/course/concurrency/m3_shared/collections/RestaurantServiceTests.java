package course.concurrency.m3_shared.collections;

import course.concurrency.exams.auction.ExecutionStatistics;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestaurantServiceTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 4_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors()*2;

    private ExecutorService executor;
    private RestaurantService service;

    @BeforeEach
    public void setup() {
        executor = Executors.newFixedThreadPool(poolSize);
        service = new RestaurantService();
    }

    @AfterAll
    public static void printStat() {
        stat.printStatistics();
    }

    @RepeatedTest(TEST_COUNT)
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                latch.await();
            } catch (InterruptedException ignored) {}

            for (int it = 0; it < iterations; it++) {
                service.getByName("A");
            }
        });

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(Set.of("A - " + iterations), service.printStat());
        stat.addData("service",end - start);
    }
}
