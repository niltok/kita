package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.CPUMonitor;
import ikuyo.utils.WindowSum;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class AppVert extends AsyncVerticle {
    static final boolean busyDetect = false;
    static final long minSwitchTime = 10 * 1000_000_000L;
    static final int detectInterval = 100, windowSize = 300;
    static final double memFactor = 0.5, cpuFactor = 0.5;

    OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
    Runtime runtime = Runtime.getRuntime();
    MessageConsumer<JsonObject> starNone;
    boolean serverEnable = true;
    WindowSum memWindow = new WindowSum(windowSize), cpuWindow = new WindowSum(windowSize);
    long prevSwitchTime = 0;

    @Override
    public void start() {
        starNone = eventBus.consumer("star.none", msg -> {
            var json = msg.body();
            switch (json.getString("type")) {
                case "star.load" -> {
                    var config = JsonObject.of("id", json.getInteger("id"));
                    await(vertx.deployVerticle(UpdateVert.class, new DeploymentOptions()
                            .setWorker(true)
                            .setConfig(config)));
                    msg.reply(JsonObject.of("type", "star.load.success"));
                }
            }
        });
        if (busyDetect) vertx.setPeriodic(detectInterval, v -> {
            memWindow.put(runtime.totalMemory() - runtime.freeMemory());
            cpuWindow.put(CPUMonitor.instance.getProcessCpu() / osMX.getAvailableProcessors());
            var currentTime = System.nanoTime();
            if (currentTime - prevSwitchTime < minSwitchTime) return;
            var memLoad = memWindow.getMean() / runtime.maxMemory();
            var cpuLoad = cpuWindow.getMean();
            var shouldEnable = memLoad < memFactor && cpuLoad < cpuFactor;
            if (serverEnable ^ shouldEnable) {
                serverEnable = shouldEnable;
                prevSwitchTime = currentTime;
                if (shouldEnable) {
                    starNone.resume();
                    logger.info(JsonObject.of("type", "server.resume",
                            "mem", memLoad, "cpu", cpuLoad));
                } else {
                    starNone.pause();
                    logger.info(JsonObject.of("type", "server.pause",
                            "mem", memLoad, "cpu", cpuLoad));
                }
            }
        });
    }
}
