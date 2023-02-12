package ikuyo.api;

import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.Map.Entry;

public class StarInfo {
    public Block[] blocks;
    public Map<Integer, StarUserInfo> starUsers;
///   层级最大值
    public int maxtier = 200;
///   层级最小值
    public int mintier = 20;
///   层级间距
    public static final double tierdistance = Math.pow(3,1.0/2)/2;

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public Buffer toBuffer() {
        return DataStatic.gzipEncode(toJson().toBuffer());
    }
    public static StarInfo gen(int seed) {
//        System.out.print("[seed]:\t");
//        System.out.println(seed);
        var info = new StarInfo();
        ArrayList<Block> block = new ArrayList<>();
        Random random = new Random(seed);

        info.maxtier = 30; info.mintier = 10;
//        生成地面最高层
        int tiernum = (int)(random.nextDouble()*(info.maxtier-info.mintier)*0.5
                        + (info.maxtier-info.mintier)*0.25)
                        + info.mintier;
//        tiernum = 13;
        int roundstarttier = (int)(tiernum * tierdistance) - 1;

//        System.out.printf("max:%d\tmin:%d%n", info.maxtier, info.mintier);

        int blocknum = info.maxtier*(info.maxtier+1)*3 - info.mintier*(info.mintier-1)*3;
        int groundnum =  tiernum*(tiernum+1)*3 - info.mintier*(info.mintier-1)*3;
        int outline_roundnum = roundstarttier*(roundstarttier+1)*3 - info.mintier*(info.mintier-1)*3;
        if (outline_roundnum < 0) outline_roundnum = 0;

//        圆角修饰部分_in
        int index_in = realIndexOf(0, info.mintier);
        double r_in = heightOf(index_in);
        int inline_tier = (int)((Math.pow(3,1.0/2)*2/3-1) * r_in) + 1 + info.mintier;
        int inline_roundnum = inline_tier*(inline_tier+1)*3 - info.mintier*(info.mintier-1)*3;
        for (var i = 0; i < inline_roundnum; i++) {
            Block newblock = new Block.Normal();
            if ( heightOf(index_in) < r_in ) newblock.type = 0;
            else {
                newblock.type = 1;
                newblock.isVisible = true;
                newblock.isDestructible = true;
                newblock.isInteractive = true;
                newblock.isCollidable = true;
            }
            block.add(newblock);
            index_in++;
        }
//        纯地面生成
        for (var i = inline_roundnum; i < outline_roundnum; i++) {
            Block newblock = new Block.Normal();
            newblock.type = 1;
            newblock.isVisible = true;
            newblock.isDestructible = true;
            newblock.isInteractive = true;
            newblock.isCollidable = true;
            block.add(newblock);
        }
//        圆角修饰部分_out
        int index_out = realIndexOf(outline_roundnum, info.mintier);
        double r_out = heightOf(tiernum*(tiernum-1)*3+1) * tierdistance;
        for (var i = outline_roundnum; i < groundnum; i++) {
            Block newblock = new Block.Normal();
            if ( heightOf(index_out) <= r_out ) {
                newblock.type = 1;
                newblock.isVisible = true;
                newblock.isDestructible = true;
                newblock.isInteractive = true;
                newblock.isCollidable = true;
            }else newblock.type = 0;
            block.add(newblock);
            index_out++;
        }
//        纯天空生成
        for (var i = groundnum; i < blocknum; i++) {
            Block newblock = new Block.Normal();
            newblock.type = 0;
            block.add(newblock);
        }

        info.blocks = block.toArray(new Block[0]);
        System.out.print("[blocks:]:\t");
        System.out.println(info.blocks.length);
        info.starUsers = new HashMap<>();
        return info;
    }

    public static StarInfo fromJson(String str) {
        return fromJson(new JsonObject(str));
    }

    public static StarInfo fromJson(Buffer buffer) {
        return fromJson(new JsonObject(DataStatic.gzipDecode(buffer)));
    }

    public static StarInfo fromJson(JsonObject json) {
        return json.mapTo(StarInfo.class);
    }

    public static class StarUserInfo {
        public double x, y;
        public boolean online;
        public StarUserInfo() {}
        public StarUserInfo(double x, double y) {
            this.x = x;
            this.y = y;
            online = true;
        }
    }

    public static void main(String[] args) {
//        StarInfo.gen(0);
//        System.out.print("angle1:\t");
//        System.out.println(StarInfo.angleOf(43));
//        System.out.print(tierOf(18));
//        System.out.printf(String.format("%g, %g, \nangle2:\t%g", pos.x, pos.y, Math.atan(pos.y/pos.x)));
//        int index = 7;
//        Position pos = StarInfo.posOf(index);
//        System.out.printf("Position at [%f,%f] is : [%d]%n", 2.3, 0.5, realIndexOf(2.3, 0.5));
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

    public static boolean is_standable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        int index = realIndexOf(x,y);
        Position pos = posOf(index);
        double rr = Math.hypot(pos.x-x, pos.y-y) + r;
        int tiers = (int)(rr / tierdistance) + 1;
        int[] blocklist = new int[tiers*(tiers+1)*3];
        for (int i = 0; i < blocklist.length; i++ ) {
            blocklist[i] = i + 1;
        }
        for (int i = 0; i < blocklist.length; i++ ) {
            Position posi = posOf(blocklist[i]);
            blocklist[i] = realIndexOf(pos.x+posi.x, pos.y+ pos.y);
        }
        for ( var i : blocklist) {
            if (star.blocks[i].isCollidable) { res = false; break; }
        }
        return res;
    }

    public static int realIndexOf(double x, double y) {
        int tier = (int)(Math.hypot(x,y)/tierdistance);
        double radian = (Math.atan2(y,x) + Math.PI*2) % (Math.PI*2);
        int edge = (int)(radian / (Math.PI/3));
        double percent = radian % (Math.PI/3);
        Map<Integer, Double> map = new HashMap<>();
        int index = edge * tier + (int)(percent * tier) + tier * (tier-1) * 3;
        Position pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index += 1;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index = edge * (tier+1) + (int)(percent * (tier+1)) + (tier+1) * tier * 3;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        index += 1;
        pos = posOf(index);
        map.put(index,Math.hypot(pos.x-x, pos.y-y));

        List<Entry<Integer, Double>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        return list.get(0).getKey();
    }
//    public static double angleOf(int realindex) {
//        int tier = tierOf(realindex);
//        int reltindex = realindex - 3*tier*(tier-1) - 1;
//        double i = (reltindex%tier)/(double)tier;
//        double x = 1 - i * Math.cos(Math.PI/3.0);
//        double y = i * Math.sin(Math.PI/3.0);
//        return Math.atan(y/x) + Math.PI*(int)(reltindex/tier)/3.0;
//    }
}
