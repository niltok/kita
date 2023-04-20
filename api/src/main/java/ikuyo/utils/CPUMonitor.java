package ikuyo.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

public class CPUMonitor {
    public static final CPUMonitor instance = new CPUMonitor();

    private final OperatingSystemMXBean osMxBean;
    private final ThreadMXBean threadBean;
    private long preTime = System.nanoTime();
    private long preUsedTime = 0;

    private static final int windowSize = 180;
    private static final WindowSum used = new WindowSum(windowSize), passed = new WindowSum(windowSize);

    private CPUMonitor() {
        osMxBean = ManagementFactory.getOperatingSystemMXBean();
        threadBean = ManagementFactory.getThreadMXBean();
    }

    public double getProcessCpu() {
        long totalTime = 0;
        for (long id : threadBean.getAllThreadIds()) {
            totalTime += threadBean.getThreadCpuTime(id);
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