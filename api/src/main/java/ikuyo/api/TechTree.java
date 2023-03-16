package ikuyo.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

/** 科技树<br/>
 *  常用函数 <br>
 *  {@link TechTree#getTechStatus(TechItem)}<br>
 *  {@link TechTree#getTrainQueue()}<br>
 *  {@link TechTree#setTrainQueue(Iterable, boolean)}<br>
 *  科技项详见 {@link TechItem} */
public class TechTree {
    public static class Data {
        /** 已训练的百分比 */
        public double trainPercent = 0;
        /** 训练完成时间(EpochMilli format)<br>
         *  -1 代表没在训练 */
        public long trainFinishAt = -1;
        public boolean isTrained() {
            return trainPercent == 1 || trainFinishAt <= Instant.now().toEpochMilli();
        }
    }
    /** 用于遍历的原始状态信息（请不要写入数据） */
    public final Map<String, Data> treeInfo;
    /** 获取某科技的训练信息 */
    public Data getData(TechItem index) {
        return treeInfo.computeIfAbsent(index.name(), i -> new Data());
    }
    /** 获取某科技的训练信息 */
    public Data getData(String index) {
        return treeInfo.computeIfAbsent(index, i -> new Data());
    }
    /** 更新科技树的训练进度 */
    public void update() {
        treeInfo.forEach((k, v) -> update(k));
    }
    /** 更新训练进度并获取训练状态 <br>
     *  1 代表已训练<br>
     *  0 代表训练中<br>
     *  -1 代表在训练队列中<br>
     *  -2 未计划训练 */
    public int update(String index) {
        var data = getData(index);
        if (data.trainPercent == 1) return 1;
        if (data.trainFinishAt == -1) return -2;
        var now = Instant.now().toEpochMilli();
        if (data.trainFinishAt <= now) {
            data.trainFinishAt = -1;
            data.trainPercent = 1;
            return 1;
        }
        var info = TechItem.get(index);
        if (info == null || info.isDisable()) return -2;
        var cost = info.cost.toMillis();
        if (cost == 0) {
            data.trainFinishAt = -1;
            data.trainPercent = 1;
            return 1;
        }
        if (data.trainFinishAt - cost >= now) return -1;
        data.trainPercent = (double) (cost - (data.trainFinishAt - now)) / cost;
        return 0;
    }
    /** 更新训练进度并获取训练状态 <br>
     *  1 代表已训练<br>
     *  0 代表训练中<br>
     *  -1 代表在训练队列中<br>
     *  -2 未计划训练 */
    public int getTechStatus(TechItem index) {
        return update(index.name());
    }
    /** 更新训练状态并获取当前训练队列 */
    public String[] getTrainQueue() {
        var res = new TreeMap<Long, String>();
        treeInfo.forEach((k, v) -> {
            var state = update(k);
            if (state < 1 && state > -2) res.put(v.trainFinishAt, k);
        });
        return res.values().toArray(String[]::new);
    }
    /** 更新训练状态并覆盖当前训练队列<br>
     * @param dependencies 是否解析依赖？如果为 true 将会自动解析依赖并插入到被依赖的训练项之前，
     * 否则会移除依赖不满足的训练项。 */
    public void setTrainQueue(Iterable<String> queue, boolean dependencies) {
        var trained = new HashSet<String>();
        treeInfo.forEach((k, v) -> {
            var state = update(k);
            if (state == 1) trained.add(k);
            v.trainFinishAt = -1;
        });
        var startTime = Instant.now().toEpochMilli();
        var trainList = new ArrayList<String>();
        for (String s : queue) {
            resolveDependency(s, trained, trainList, dependencies);
        }
        for (String s : trainList) {
            var data = getData(s);
            var info = TechItem.get(s);
            if (info == null || info.isDisable()) continue;
            var cost = info.cost.toMillis();
            var restCost = cost - (long) (data.trainPercent * cost);
            startTime += restCost;
            data.trainFinishAt = startTime;
        }
    }
    void resolveDependency(String s, Set<String> trained, List<String> trainList, boolean recursion) {
        var info = TechItem.get(s);
        if (info == null || info.isDisable()) return;
        for (var dep : info.dependencies) {
            if (trained.contains(dep.name())) continue;
            // 处理依赖
            if (!recursion) return;
            resolveDependency(dep.name(), trained, trainList, true);
        }
        trained.add(s);
        trainList.add(s);
    }
    public TechTree() {
        treeInfo = new HashMap<>();
    }
    public TechTree(String json) throws JsonProcessingException {
        var mapper = new ObjectMapper();
        treeInfo = mapper.readValue(json, new TypeReference<>() {
        });
    }
    public String toString() {
        var mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(treeInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
