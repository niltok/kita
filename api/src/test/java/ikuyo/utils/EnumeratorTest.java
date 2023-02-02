package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static ikuyo.utils.AsyncStatic.delay;
import static org.junit.jupiter.api.Assertions.*;

public class EnumeratorTest implements AsyncHelper, TestHelper {
    @Test
    public void testNext() {
        runAsync(() -> {
            var nats = new Enumerator<Integer, Void>(ctx -> {
                var i = 0;
                while (true) {
                    ctx.yield(i);
                    i++;
                }
            });
            assertEquals(0, nats.next(null));
            assertEquals(1, nats.next(null));
            assertEquals(2, nats.next(null));
        });
    }
    @Test
    public void testTryNext() {
        runAsync(() -> {
            var fin3 = new Enumerator<Integer, Void>(ctx -> {
                for (var i = 0; i < 3; i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ctx.yield(i);
                }
            });
            var complete = Promise.promise();
            async(() -> {
                await(fin3.completeFuture());
                complete.complete();
            });
            assertEquals(0, fin3.next(null));
            assertNull(fin3.tryNext(null));
            assertEquals(1, fin3.next(null));
            assertTrue(fin3.hasNext());
            assertFalse(complete.future().isComplete());
            assertEquals(2, fin3.next(null));
            assertFalse(fin3.hasNext());
            assertTrue(complete.future().isComplete());
        });
    }
    @Test
    public void testEcho() {
        runAsync(() -> {
            var echo = new Enumerator<Integer, Integer>(ctx -> {
                var i = 0;
                while (true) {
                    i = ctx.yield(i);
                }
            });
            assertEquals(0, echo.next(3));
            assertEquals(3, echo.next(5));
            assertEquals(5, echo.next(0));
        });
    }
}
