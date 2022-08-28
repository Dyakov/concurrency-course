package course.concurrency.m3_shared.practice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

class ConcurrentHashMapDeadlockTests {

    private Map<CollisionKey, Integer> map = new ConcurrentHashMap<>();

    @BeforeEach
    void setup() {
        map.put(new CollisionKey(1), 1);
        map.put(new CollisionKey(2), 2);
    }

    @Test
    void shouldDeadlock() {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(2);
        Runnable task1 = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Task 1 started");
            map.compute(new CollisionKey(1), (k, v) ->
                {
                    map.compute(new CollisionKey(2), (k1, v1) -> v + k.getValue());
                    return v + k.getValue();
                });
            latch2.countDown();
            System.out.println("Task 1 finished");
        };
        Runnable task2 = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Task 2 started");
            map.compute(new CollisionKey(2), (k, v) ->
            {
                map.compute(new CollisionKey(1), (k1, v1) -> v + k.getValue());
                return v + k.getValue();
            });
            latch2.countDown();
            System.out.println("Task 2 finished");
        };
        new Thread(task1).start();
        new Thread(task2).start();
        latch.countDown();
        try {
            latch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Test finished");
    }
}