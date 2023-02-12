package ikuyo.api;

import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StarInfo {
    public Block[] blocks;
    public Map<Integer, StarUserInfo> starUsers;
///   层级最大值
    public int maxtier = 50;
///   层级最小值
    public int mintier = 10;

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

//        生成地面最高层
        int tiernum = (int)(random.nextDouble()*(info.maxtier-info.mintier)*0.5
                        + (info.maxtier-info.mintier)*0.25)
                        + info.mintier;
//        tiernum = 13; info.maxtier = 13; info.mintier = 10;
        int roundstarttier = (int)(tiernum * (Math.pow(3,1.0/2)/2)) - 1;

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
            block.add(newblock);
        }
//        圆角修饰部分_out
        int index_out = realIndexOf(outline_roundnum, info.mintier);
        double r_out = heightOf(tiernum*(tiernum-1)*3+1) * Math.pow(3,1.0/2) / 2;
        for (var i = outline_roundnum; i < groundnum; i++) {
            Block newblock = new Block.Normal();
            if ( heightOf(index_out) <= r_out ) {
                newblock.type = 1;
                newblock.isVisible = true;
                newblock.isDestructible = true;
                newblock.isInteractive = true;
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
//        Position pos = StarInfo.posOf(43);
//        System.out.print("angle1:\t");
//        System.out.println(StarInfo.angleOf(43));
//        System.out.print(tierOf(18));
//        System.out.printf(String.format("%g, %g, \nangle2:\t%g", pos.x, pos.y, Math.atan(pos.y/pos.x)));
//        System.out.println(heightOf(7));
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
//    public static double angleOf(int realindex) {
//        int tier = tierOf(realindex);
//        int reltindex = realindex - 3*tier*(tier-1) - 1;
//        double i = (reltindex%tier)/(double)tier;
//        double x = 1 - i * Math.cos(Math.PI/3.0);
//        double y = i * Math.sin(Math.PI/3.0);
//        return Math.atan(y/x) + Math.PI*(int)(reltindex/tier)/3.0;
//    }
}
