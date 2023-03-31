package ikuyo.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.equipments.AbstractWeapon;
import ikuyo.api.spaceships.AbstractSpaceship;
import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class StarInfo {
    public Block[] blocks;
    public Map<Integer, StarUserInfo> starUsers;
    /**层级最大值*/
    public static final int maxTier = 1000;
    /**<p>层级最小值<p/>
     * [Warn]: Plz make sure mintier > 0*/
    public static final int minTier = 10;
    /**层级间距*/
    public static final double tierDistance = Math.sqrt(3)/2;
    /**六边形块边长*/
    public static final double edgeLength = 1 / Math.sqrt(3);
    /**星球半径*/
    protected double star_r;

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Buffer toBuffer() {
        try {
            return DataStatic.gzipEncode(new ObjectMapper().writeValueAsBytes(this));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public static StarInfo gen(int seed) {
        var info = new StarInfo();
        Random random = new Random(seed);
        int blockNum = maxTier *(maxTier +1)*3 - minTier *(minTier -1)*3;
        info.blocks = new Block[blockNum];
        for (int i = 0;i < info.blocks.length; i++) {
            info.blocks[i] = new Block.Normal();
            info.blocks[i].variant = 0;
        }
        info.starUsers = new HashMap<>();
        int baseTier = 50;
        double noiseLength = 30.0;

//        System.out.println("[blocks:]: %d".formatted(info.blocks.length));
//        System.out.printf("max:%d\tmin:%d%n", info.maxTier, info.minTier);


 //        圆角修饰部分_in
        int index_in = realIndexOf(0, minTier);
        double r_in = heightOf(index_in);
        int inline_tier = (int)((Math.sqrt(3)*2/3-1) * r_in) + 1 + minTier;
        int inline_roundnum = inline_tier*(inline_tier+1)*3 - minTier *(minTier -1)*3;
        if (inline_roundnum > blockNum) inline_roundnum = blockNum;
        for (var i = 0; i < inline_roundnum; i++) {
            if ( heightOf(index_in) < r_in ) info.blocks[i].type = 0;
            else {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
            }
            index_in++;
        }

//        计算地表
        int tierNum = (int)(random.nextDouble()*(maxTier - minTier)*0.5
                + (maxTier - minTier)*0.25)
                + minTier;
        int roundStartTier = (int)(tierNum * tierDistance) - 1;
        int groundNum =  (tierNum + baseTier) * ((tierNum+baseTier)+1) * 3 - minTier * (minTier -1) * 3;
        int outline_roundnum = roundStartTier*(roundStartTier+1)*3 - minTier *(minTier -1)*3;
        if (outline_roundnum < 0) outline_roundnum = 0;

//        纯地面生成
        for (var i = inline_roundnum; i < outline_roundnum; i++) {
            info.blocks[i].type = 1;
            info.blocks[i].isVisible = true;
            info.blocks[i].isDestructible = true;
            info.blocks[i].isInteractive = true;
            info.blocks[i].isCollisible = true;
        }

//        圆角修饰部分_out
        int index_out = realIndexOf(outline_roundnum, minTier);
        info.star_r = heightOf(tierNum*(tierNum-1)*3+1) * tierDistance;
        double dropHeight = baseTier * StarInfo.tierDistance;

        Range range = new Range(random.nextLong());
        long _seed = random.nextLong();
        for (var i = outline_roundnum; i < groundNum; i++) {
            double percent = angleOf(index_out) / Math.PI / 2.0;
            double Random = range.Random(percent);
            if ( (heightOf(index_out) - info.star_r) * 2 / dropHeight
                    < (OpenSimplex2S.noise2(_seed, percent * noiseLength, 0) + 1) * Random) {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
            }else { info.blocks[i].type = 0; info.blocks[i].variant = 0; }
            index_out++;

//            if (range.test) { info.blocks[i].type = 40; info.blocks[i].variant = 4;}
//            System.out.println("[random]: %f".formatted(Random));
        }

//        表面

        for (var i: surfaceBlocks(0, minTier, inline_tier, info)) {
            info.blocks[i].isDestructible = false;
            info.blocks[i].isInteractive = false;
            info.blocks[i].isSurface = true;
        }

        for (var i: surfaceBlocks(0, roundStartTier, tierNum + baseTier, info)) {
            info.blocks[i].type = 1;
            info.blocks[i].variant = 1;
            info.blocks[i].isSurface = true;
        }


//        纯天空生成
//        for (var i = groundNum; i < blockNum; i++) {
//            info.blocks[i].type = 0;
//        }

//        石头
        int index = realIndexOf(0, minTier);
        _seed = random.nextLong();
        range = new Range(random.nextLong());
        for (int i = 0; i < groundNum; i++) {
            if (info.blocks[i].isVisible) {
                Position pos = posOf(index);
                double height = Math.hypot(pos.x, pos.y);
                double percent = angleOf(index) / Math.PI / 2.0;
                double Random = range.Random(percent);
                if ((OpenSimplex2S.noise2(_seed, pos.x / 10, pos.y / 10) + 1)
                        > 2 * (Math.atan((height / info.star_r - (0.8 + 0.2 * Random)) * 10) + Math.PI / 2.0) / Math.PI) {
                    info.blocks[i].type = 2;
                    info.blocks[i].variant = 0;
                }
            }
            index++;
        }

//        int[] test = new int[500];
//        boolean a = true;
//        for(int i = 0; i < info.blocks.length; i++) {
//            int tier = tierOf(realIndexOf(i,info.mintier));
//            if ( tier > 260 || tier < 240 ) { a = false; }
//            if(!info.blocks[i].isVisible) {
//                test[tierOf(realIndexOf(i,info.mintier))] ++;
//            }
//        }
//        for (int i = 0; i < 500; i++) {
//            if (test[i] != 0) System.out.println("[tier]: %d, [num]: %d".formatted(i,test[i]));
//        }

//        for (var i: info.blocks) {
//            if (i.isSurface) { i.type = 40; i.variant = 4;}
//        }

        return info;
    }

    public static StarInfo fromJson(String str) {
        return fromJson(new JsonObject(str));
    }

    public static StarInfo fromJson(Buffer buffer) {
        try {
            return new ObjectMapper().readValue(DataStatic.gzipDecode(buffer), StarInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static StarInfo fromJson(JsonObject json) {
        return json.mapTo(StarInfo.class);
    }

    public static class StarUserInfo {
        public double x, y = maxTier;

        public double rotation;
        public boolean online;
        public AbstractWeapon weapon;
        public double san = 100;
        public AbstractSpaceship spaceship = new AbstractSpaceship(CargoStatic.shuttle.type());
        public String controlType = "walk";
        public StarUserInfo() {
            spaceship.weapons[0] = new AbstractWeapon(CargoStatic.defaultWeapon.type());
            weapon = spaceship.weapons[0];
        }
        public StarUserInfo(double x, double y) {
            this();
            this.x = x;
            this.y = y;
            online = true;
        }
    }

    public static void main(String[] args) {
//        for (int i = 0; i < 100; i++) {
//            System.out.println(OpenSimplex2S.noise2(0, i, 0) + 1);
//        }
//        System.out.println(OpenSimplex2S.noise2(0, 6, 0) + 1);
//        System.out.println(Math.tan(0.8 * Math.PI / 2));
    }

    public static int realIndexOf(int index, int mintier) {
        return 3*mintier*(mintier-1) + index + 1;
    }

    public static int tierOf(int realIndex) {
        double result = 0.5 + Math.sqrt(12*realIndex+9)/6.0;
        int tier = (int)result;
        if (result - tier == 0.0) tier--;
        return tier;
    }

//    单位 1 : 根3边长;  两层距离 : 二分之根3
    public static Position posOf(int realIndex) {
        if (realIndex == 0) return new Position(0, 0);
        Position pos = new Position();
        int tier = tierOf(realIndex);
        int relativeIndex = realIndex - 3*tier*(tier-1) - 1;
        double i = (relativeIndex%tier) / (double)tier;
        double x = 1 - i * Math.cos(Math.PI/3.0);
        double y = i * Math.sin(Math.PI/3.0);
        double l = Math.hypot(x,y);
        double angle = Math.atan(y / x) + Math.PI * (double)(relativeIndex / tier) / 3.0;
        pos.x = Math.cos(angle) * l * tier;
        pos.y = Math.sin(angle) * l * tier;
        return pos;
    }

    public static double heightOf(int realIndex) {
        Position pos = posOf(realIndex);
        return Math.hypot(pos.x, pos.y);
    }

    /**return angle in the range of 0 to 2pi.*/
    public static double angleOf(int realIndex) {
        Position pos = posOf(realIndex);
        return (Math.atan2(pos.y, pos.x) + Math.PI*2) % (Math.PI*2);
    }

    public static boolean isStandable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        if (Math.hypot(x,y) < maxTier * tierDistance) {
            int index = realIndexOf(x, y);
//            System.out.println("[index]: %d".formatted(index));
            Position pos = posOf(index);
            double rr = Math.hypot(pos.x - x, pos.y - y) + r;
            int tiers = (int) (rr / tierDistance) + 1;

            int[] blocklist = nTierAround(index, tiers)
                    .stream().mapToInt(Integer::valueOf).toArray();

//            System.out.println("[r]: %f, [rr]: %f, [tier]: %d, [blocks]: %d".formatted(r, rr, tiers, blocklist.length));
//            for (var i : blocklist) { System.out.println(i); }

            for (var i : blocklist) {
                if (star.blocks[i].isCollisible) {
                    Position posI = posOf(i);
                    if (Math.hypot(posI.x-x,posI.y-y) < r) res = false;
                    break;
                }
            }
        }
        return res;
    }
    public static int indexOf(int realIndex, int minTier) {
        return realIndex - minTier * (minTier - 1) * 3 - 1;
    }

    public static int realIndexOf(double x, double y) {
        double radian = (Math.atan2(y,x) + Math.PI*2) % (Math.PI*2);
        radian = Double.parseDouble(String.format("%.6f", radian));
        double PIDiv3 = Double.parseDouble(String.format("%.6f", Math.PI/3));
        int edge = (int)(radian / PIDiv3);
        double i = radian % PIDiv3;
        int tier = (int)(Math.cos(Math.abs(Math.PI/6-i))*Math.hypot(x,y) / tierDistance);
        double percent = (Math.tan(i)/Math.sin(Math.PI/3)) / (Math.tan(i)/Math.tan(Math.PI/3)+1);
        if (percent > 1) percent -= 1.0;

//        System.out.println("{realIndexOf}\t[tier]: %d, [edge]: %d, [i]: %f, [percent]: %f"
//                .formatted(tier, edge, i, percent));

        Map<Integer, Double> map = new HashMap<>();
        if (tier == 0) map.put(0,Math.hypot(x, y));
        for (i = 0; i < 2; i++) {
            int index = edge * tier + (int)(percent * tier) + tier * (tier - 1) * 3 + 1;
            Position pos = posOf(index);
            map.put(index, Math.hypot(pos.x - x, pos.y - y));

            if ((index + 1) / (double)(tier * (tier + 1) * 3) <= 1) index += 1;
            else index = (tier - 1) * tier * 3 + 1;
            pos = posOf(index);
            map.put(index, Math.hypot(pos.x - x, pos.y - y));
            tier++;
        }

        List<Entry<Integer, Double>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

//        for (var v: list) {
//            System.out.println("[K]: %d, [V]: %f".formatted(v.getKey(), v.getValue()));
//        }

        return list.get(0).getKey();
    }

/** <p>realIndex 周围 n 层的块的真实编号<p/>
 * 返回的List中存储可以直接调用的 index , 而不是 realIndex*/
    public static ArrayList<Integer> nTierAround(int realIndex, int n) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realIndex);
        int extraBlock = minTier * (minTier - 1) * 3;
        for (int i = 0; i < n * (n + 1) * 3; i++) {
            Position posI = posOf(i+1);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);
            if( tier >= minTier && tier <= maxTier) res.add(index - extraBlock - 1);
        }
        return res;
    }

    public static ArrayList<Integer> nTierAround(Position position, double r) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realIndexOf(position.x, position.y));
        int extraBlock = minTier * (minTier - 1) * 3;
        int nTier = (int)((r + edgeLength) / tierDistance) + 1;
        for (int i = 0; i < nTier * (nTier + 1) * 3 + 1; i++) {
            Position posI = posOf(i);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);
            if (tier >= minTier && tier <= maxTier && position.distance(posOf(index)) <= r)
                res.add(index - extraBlock - 1);
        }
        return res;
    }

    public static ArrayList<Integer> surfaceBlocks(int realIndex, int startTier, int endTier, StarInfo star) {
        ArrayList<Integer> result = new ArrayList<>();
        Position pos = posOf(realIndex);

        for (int i = (startTier <= 0 ? 0 : startTier * (startTier - 1) * 3 + 1);
             i < endTier * (endTier + 1) * 3 + 1; i++) {
            Position posI = posOf(i);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);

            if( tier >= minTier && tier <= maxTier) {
                if (star.blocks[indexOf(index, minTier)].isVisible) {
                    int[] blocklist = nTierAround(index, 1)
                            .stream().mapToInt(Integer::valueOf).toArray();
                    for (var b : blocklist) {
                        if (star.blocks[b].type == 0) {
                            result.add(indexOf(index, minTier));
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

//    /**创建属于你的星球
//     * @param seed random
//     * */
//    public static StarInfo CreatMyStar(int seed) {
//        StarInfo MyStar = new StarInfo();
//        StarInfo.design = MyStar;
//        MyStar = gen(seed);
//        return MyStar;
//    }

}