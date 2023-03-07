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
    public int maxtier = 500;
    /**层级最小值*/
    public int mintier = 10;
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
        int blocknum = info.maxtier*(info.maxtier+1)*3 - info.mintier*(info.mintier-1)*3;
        info.blocks = new Block[blocknum];
        for (int i = 0;i < info.blocks.length; i++) {
            info.blocks[i] = new Block.Normal();
        }
        info.starUsers = new HashMap<>();
        int basetier = 30;
        double noiselength = 30.0;

//        System.out.println("[blocks:]: %d".formatted(info.blocks.length));
//        System.out.printf("max:%d\tmin:%d%n", info.maxtier, info.mintier);


 //        圆角修饰部分_in
        int index_in = realIndexOf(0, info.mintier);
        double r_in = heightOf(index_in);
        int inline_tier = (int)((Math.pow(3,1.0/2)*2/3-1) * r_in) + 1 + info.mintier;
        int inline_roundnum = inline_tier*(inline_tier+1)*3 - info.mintier*(info.mintier-1)*3;
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
        int tiernum = (int)(random.nextDouble()*(info.maxtier-info.mintier)*0.5
                + (info.maxtier-info.mintier)*0.25)
                + info.mintier;
        int roundstarttier = (int)(tiernum * tierdistance) - 1;
        int groundnum =  (tiernum + basetier) * ((tiernum+basetier)+1) * 3 - info.mintier * (info.mintier-1) * 3;
        int outline_roundnum = roundstarttier*(roundstarttier+1)*3 - info.mintier*(info.mintier-1)*3;
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
        int index_out = realIndexOf(outline_roundnum, info.mintier);
        info.star_r = heightOf(tiernum*(tiernum-1)*3+1) * tierdistance;
        double dropheight = basetier * StarInfo.tierdistance;
        for (var i = outline_roundnum; i < groundnum; i++) {
            if ( (heightOf(index_out) - info.star_r) * 2 / dropheight
                    < OpenSimplex2S.noise2(seed, (angleOf(index_out) / Math.PI * 2) * noiselength, 0) + 1 ) {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollidable = true;
            }else info.blocks[i].type = 0;
            index_out++;
        }

        index_out = realIndexOf(outline_roundnum, info.mintier);
        int surblock = info.mintier * (info.mintier - 1) * 3;
        for (var i = outline_roundnum; i < groundnum; i++) {
            int[] blocklist = ntierAround(index_out, 1, info.mintier, info.maxtier)
                    .stream().mapToInt(Integer::valueOf).toArray();
            for (var b: blocklist ) {
                if (info.blocks[b-surblock-1].type == 0) {
                    info.blocks[i].type = 11;
                    break;
                }
            }
            index_out++;
        }


//        纯天空生成
        for (var i = groundnum; i < blocknum; i++) {
            info.blocks[i].type = 0;
        }

//        地洞
//        int index = realIndexOf(0, info.mintier);
//        for (int i = 0; i < info.blocks.length; i++) {
//            Position pos = posOf(index);
//            if ( OpenSimplex2S.noise2(seed, pos.x/10, pos.y/10) * 2 > 1 ) {
//                info.blocks[i] = new Block.Normal();
//                info.blocks[i].type = 0;
//            }
//            index++;
//        }

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
        public double x, y = 300 * Drawable.scaling;
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
    }

    public static int realIndexOf(int index, int mintier) {
        return 3*mintier*(mintier-1) + index + 1;
    }

    public static int tierOf(int realindex) {
        double an = 0.5 + Math.pow((12*realindex+9), 1.0/2)/6.0;
        int res = (int)an;
        if (an-res == 0.0) res--;
        return res;
    }

//    单位 1 : 根3边长;  两层距离 : 二分之根3
    public static Position posOf(int realindex) {
        Position pos = new Position();
        int tier = tierOf(realindex);
        int reltindex = realindex - 3*tier*(tier-1) - 1;
        double i = (reltindex%tier)/(double)tier;
        double x = 1 - i * Math.cos(Math.PI/3.0);
        double y = i * Math.sin(Math.PI/3.0);
        double l = Math.hypot(x,y);
        double angle = Math.atan(y/x) + Math.PI*(reltindex/tier)/3.0;
        pos.x = Math.cos(angle) * l * tier;
        pos.y = Math.sin(angle) * l * tier;
        return pos;
    }

    public static double heightOf(int realindex) {
        Position pos = posOf(realindex);
        return Math.hypot(pos.x, pos.y);
    }

    public static double angleOf(int realindex) {
        Position pos = posOf(realindex);
        return (Math.atan2(pos.y, pos.x) + Math.PI*2) % (Math.PI*2);
    }

    public static boolean is_standable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        if (Math.hypot(x,y) < star.maxtier*tierdistance) {
            int index = realIndexOf(x, y);
//            System.out.println("[index]: %d".formatted(index));
            Position pos = posOf(index);
            double rr = Math.hypot(pos.x - x, pos.y - y) + r;
            int tiers = (int) (rr / tierdistance) + 1;

            int[] blocklist = ntierAround(index, tiers, star.mintier, star.maxtier)
                    .stream().mapToInt(Integer::valueOf).toArray();

//            System.out.println("[r]: %f, [rr]: %f, [tier]: %d, [blocks]: %d".formatted(r, rr, tiers, blocklist.length));
//            for (var i : blocklist) { System.out.println(i); }

            int surblock = star.mintier * (star.mintier - 1) * 3;
            for (var i : blocklist) {
                if (star.blocks[i-surblock-1].isCollidable) {
                    Position posi = posOf(i);
                    if (Math.hypot(posi.x-x,posi.y-y) < r) res = false;
                    break;
                }
            }
        }
        return res;
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
        int index = edge * tier + (int)(percent * tier) + tier * (tier-1) * 3 + 1;
        Position pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index += 1;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index = edge * (tier+1) + (int)(percent * (tier+1)) + (tier+1) * tier * 3 + 1;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index += 1;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        List<Entry<Integer, Double>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        return list.get(0).getKey();
    }

/** realindex 周围 n 层的块的真实编号*/
    public static ArrayList<Integer> ntierAround(int realindex, int n, int mintier, int maxtier) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realindex);
        for (int i = 0; i < n*(n+1)*3; i++) {
            Position posi = posOf(i+1);
            int index = realIndexOf(pos.x + posi.x, pos.y + posi.y);
            int tier = tierOf(index);
            if( tier >= mintier && tier <= maxtier) res.add(index);
        }
        return res;
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
