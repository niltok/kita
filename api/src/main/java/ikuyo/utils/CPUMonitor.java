package ikuyo.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class CPUMonitor {
    public static final CPUMonitor instance = new CPUMonitor();

    private final ThreadMXBean threadBean;
    private long preTime = System.nanoTime();
    private long preUsedTime = 0;

    private static final int windowSize = 30;
    private static final WindowSum used = new WindowSum(windowSize), passed = new WindowSum(windowSize);

    private CPUMonitor() {
        threadBean = ManagementFactory.getThreadMXBean();
    }

    public double getProcessCpu() {
        if (System.nanoTime() - preTime <= 100 * 1000_000)
            return (used.getSum() / passed.getSum());
        long totalTime = 0;
        for (long id : threadBean.getAllThreadIds()) {
            var cpuTime = threadBean.getThreadCpuTime(id);
            if (cpuTime >= 0) totalTime += cpuTime;
        }
        long curtime = System.nanoTime();
        long usedTime = totalTime - preUsedTime;
        preUsedTime = totalTime;
        long totalPassedTime = curtime - preTime;
        preTime = curtime;
        used.put(usedTime);
        passed.put(totalPassedTime);
        return (used.getSum() / passed.getSum());
    }
}