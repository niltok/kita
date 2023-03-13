package ikuyo.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public int maxTier = 500;
    /**<p>层级最小值<p/>
     * [Warn]: Plz make sure mintier > 0*/
    public int minTier = 10;
    /**层级间距*/
    public static final double tierdistance = Math.pow(3,1.0/2)/2;
    /**星球半径*/
    public double star_r;

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
        int blocknum = info.maxTier *(info.maxTier +1)*3 - info.minTier *(info.minTier -1)*3;
        info.blocks = new Block[blocknum];
        for (int i = 0;i < info.blocks.length; i++) {
            info.blocks[i] = new Block.Normal();
            info.blocks[i].variant = 0;
        }
        info.starUsers = new HashMap<>();
        int basetier = 50;
        double noiselength = 30.0;

//        System.out.println("[blocks:]: %d".formatted(info.blocks.length));
//        System.out.printf("max:%d\tmin:%d%n", info.maxtier, info.mintier);


 //        圆角修饰部分_in
        int index_in = realIndexOf(0, info.minTier);
        double r_in = heightOf(index_in);
        int inline_tier = (int)((Math.pow(3,1.0/2)*2/3-1) * r_in) + 1 + info.minTier;
        int inline_roundnum = inline_tier*(inline_tier+1)*3 - info.minTier *(info.minTier -1)*3;
        if (inline_roundnum > blocknum) inline_roundnum = blocknum;
        for (var i = 0; i < inline_roundnum; i++) {
            if ( heightOf(index_in) < r_in ) info.blocks[i].type = 0;
            else {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollidable = true;
            }
            index_in++;
        }

//        计算地表
        int tiernum = (int)(random.nextDouble()*(info.maxTier -info.minTier)*0.5
                + (info.maxTier -info.minTier)*0.25)
                + info.minTier;
        int roundstarttier = (int)(tiernum * tierdistance) - 1;
        int groundnum =  (tiernum + basetier) * ((tiernum+basetier)+1) * 3 - info.minTier * (info.minTier -1) * 3;
        int outline_roundnum = roundstarttier*(roundstarttier+1)*3 - info.minTier *(info.minTier -1)*3;
        if (outline_roundnum < 0) outline_roundnum = 0;

//        纯地面生成
        for (var i = inline_roundnum; i < outline_roundnum; i++) {
            info.blocks[i].type = 1;
            info.blocks[i].isVisible = true;
            info.blocks[i].isDestructible = true;
            info.blocks[i].isInteractive = true;
            info.blocks[i].isCollidable = true;
        }

//        圆角修饰部分_out
        int index_out = realIndexOf(outline_roundnum, info.minTier);
        info.star_r = heightOf(tiernum*(tiernum-1)*3+1) * tierdistance;
        double dropheight = basetier * StarInfo.tierdistance;

        Range range = new Range(random.nextLong());
        long _seed = random.nextLong();
        for (var i = outline_roundnum; i < groundnum; i++) {
            double percent = angleOf(index_out) / Math.PI / 2.0;
            double Random = range.Random(percent);
            if ( (heightOf(index_out) - info.star_r) * 2 / dropheight
                    < (OpenSimplex2S.noise2(_seed, percent * noiselength, 0) + 1) * Random) {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollidable = true;
            }else { info.blocks[i].type = 0; info.blocks[i].variant = 0; }
            index_out++;

//            if (range.test) { info.blocks[i].type = 40; info.blocks[i].variant = 4;}
//            System.out.println("[random]: %f".formatted(Random));
        }

//        表面
        index_in = realIndexOf(0, info.minTier);
        for (var i = 0; i < inline_roundnum; i++) {
            if (info.blocks[i].isVisible) {
                int[] blocklist = nTierAround(index_in, 1, info.minTier, info.maxTier)
                        .stream().mapToInt(Integer::valueOf).toArray();
                for (var b : blocklist) {
                    if (info.blocks[b].type == 0) {
                        info.blocks[i].isDestructible = false;
                        info.blocks[i].isInteractive = false;
                        break;
                    }
                }
            }
            index_in++;
        }

        index_out = realIndexOf(outline_roundnum, info.minTier);
        for (var i = outline_roundnum; i < groundnum; i++) {
            if (info.blocks[i].isVisible) {
                int[] blocklist = nTierAround(index_out, 1, info.minTier, info.maxTier)
                        .stream().mapToInt(Integer::valueOf).toArray();
                for (var b : blocklist) {
                    if (info.blocks[b].type == 0) {
                        info.blocks[i].type = 1;
                        info.blocks[i].variant = 1;
                        break;
                    }
                }
            }
            index_out++;
        }


//        纯天空生成
        for (var i = groundnum; i < blocknum; i++) {
            info.blocks[i].type = 0;
        }

//        石头
        int index = realIndexOf(0, info.minTier);
        _seed = random.nextLong();
        range = new Range(random.nextLong());
        for (int i = 0; i < groundnum; i++) {
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
        public double x, y = 300;
        public boolean online;
        public StarUserInfo() {}
        public StarUserInfo(double x, double y) {
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
        double result = 0.5 + Math.pow((12*realIndex+9), 1.0/2)/6.0;
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

    public static double heightOf(int realindex) {
        Position pos = posOf(realindex);
        return Math.hypot(pos.x, pos.y);
    }

    /**ruturn angle in the range of 0 to 2pi.*/
    public static double angleOf(int realindex) {
        Position pos = posOf(realindex);
        return (Math.atan2(pos.y, pos.x) + Math.PI*2) % (Math.PI*2);
    }

    public static boolean isStandable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        if (Math.hypot(x,y) < star.maxTier *tierdistance) {
            int index = realIndexOf(x, y);
//            System.out.println("[index]: %d".formatted(index));
            Position pos = posOf(index);
            double rr = Math.hypot(pos.x - x, pos.y - y) + r;
            int tiers = (int) (rr / tierdistance) + 1;

            int[] blocklist = nTierAround(index, tiers, star.minTier, star.maxTier)
                    .stream().mapToInt(Integer::valueOf).toArray();

//            System.out.println("[r]: %f, [rr]: %f, [tier]: %d, [blocks]: %d".formatted(r, rr, tiers, blocklist.length));
//            for (var i : blocklist) { System.out.println(i); }

            for (var i : blocklist) {
                if (star.blocks[i].isCollidable) {
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
        int tier = (int)(Math.cos(Math.abs(Math.PI/6-i))*Math.hypot(x,y) / tierdistance);
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
    public static ArrayList<Integer> nTierAround(int realIndex, int n, int minTier, int maxTier) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realIndex);
        int extraBlock = minTier * (minTier - 1) * 3;
        for (int i = 0; i < n*(n+1)*3; i++) {
            Position posI = posOf(i+1);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);
            if( tier >= minTier && tier <= maxTier) res.add(index - extraBlock - 1);
        }
        return res;
    }

    public static ArrayList<Integer> SurfaceBlocks(int realIndex, int startTier, int endTier, StarInfo star) {
        ArrayList<Integer> result = new ArrayList<>();
        Position pos = posOf(realIndex);

        for (int i = (startTier == 0 ? 0 : startTier * (startTier - 1) * 3 + 1);
             i < endTier * (endTier + 1) * 3 + 1; i++) {
            Position posI = posOf(i);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);

            if( tier >= star.minTier && tier <= star.maxTier) {
                if (star.blocks[indexOf(index, star.minTier)].isVisible) {
                    int[] blocklist = nTierAround(index, 1, star.minTier, star.maxTier)
                            .stream().mapToInt(Integer::valueOf).toArray();
                    for (var b : blocklist) {
                        if (star.blocks[b].type == 0) {
                            result.add(indexOf(index, star.minTier));
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