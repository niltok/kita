package ikuyo.api.techtree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 科技树<br/>
 *  常用函数 <br>
 *  {@link TechTree#train(TechLevel)}<br>
 *  {@link TechTree#trainTime(TechLevel)}<br>
 *  科技项详见 {@link TechItem} */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TechTree {
    public static class Data implements Cloneable {
        /** 已训练等级 */
        public long level = 0;

        @Override
        public Data clone() {
            try {
                Data clone = (Data) super.clone();
                // clone recursively
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
    /** 用于遍历的原始状态信息（请不要写入数据） */
    public final Map<String, Data> treeInfo;
    private long epochRest = Instant.now().toEpochMilli();
    /** 更新训练状态并覆盖当前训练队列<br>
     * @param tech 训练科技，将会自动解析依赖
     * @return 训练是否成功
     * */
    public boolean train(TechLevel tech) {
        var trainList = resolveDependency(tech);
        var totalTime = getTrainTime(trainList);
        if (Instant.now().toEpochMilli() - epochRest < totalTime) return false;
        epochRest += totalTime;
        for (var item : trainList) {
            treeInfo.computeIfAbsent(item.techItem.name(), i -> new Data()).level = item.level;
        }
        return true;
    }

    public long trainTime(TechLevel tech) {
        var trainList = resolveDependency(tech);
        return getTrainTime(trainList);
    }

    public long availableTime() {
        return Instant.now().toEpochMilli() - epochRest;
    }

    public boolean canTrain(TechLevel tech) {
        return trainTime(tech) <= availableTime();
    }

    private static long getTrainTime(List<TechLevel> trainList) {
        return trainList.stream().mapToLong(s -> {
            var info = TechItem.get(s.techItem.name());
            if (info == null || !info.enable) return 0;
            return info.cost.apply(s.level).toMillis();
        }).sum();
    }

    private List<TechLevel> resolveDependency(TechLevel s) {
        return resolveDependency(s, new HashMap<>(), new ArrayList<>());
    }

    private List<TechLevel> resolveDependency(TechLevel s, Map<String, Data> trained, List<TechLevel> trainList) {
        var info = TechItem.get(s.techItem.name());
        if (info == null || !info.enable || s.level > info.maxLevel) return trainList;
        var tech = trained.computeIfAbsent(s.techItem.name(), i -> treeInfo.getOrDefault(i, new Data()).clone());
        if (tech.level >= s.level) return trainList;
        resolveDependency(new TechLevel(s.techItem, s.level - 1), trained, trainList);
        for (var dep : info.dependencies) {
            resolveDependency(dep, trained, trainList);
        }
        trained.get(s.techItem.name()).level = s.level;
        trainList.add(s);
        return trainList;
    }
    public TechTree() {
        treeInfo = new HashMap<>();
    }

    public static TechTree fromJson(String json) {
        var mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, TechTree.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        var mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
