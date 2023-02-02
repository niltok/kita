package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.await.impl.VirtualThreadContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AsyncHelperTest implements AsyncHelper, TestHelper {
    @Test
    void asyncTest() {
        runAsync(() -> {
            async(() -> Assertions.assertInstanceOf(VirtualThreadContext.class, Vertx.currentContext()));
        });
    }
}
