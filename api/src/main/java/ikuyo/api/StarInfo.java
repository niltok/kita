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
    public int maxtier = 10;
///   层级最小值
    public int mintier = 5;

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
        var info = new StarInfo();

        ArrayList<Block> block = new ArrayList<>();
        Random random = new Random(seed);

        int tiernum = (int)(random.nextDouble()*(info.maxtier-info.mintier)) + info.mintier;
//        tiernum = 200; info.mintier = 150;
//        System.out.printf("max:%d\tmin:%d%n", info.maxtier, info.mintier);
        int blocknum = tiernum*(tiernum+1)*3 + info.mintier*(info.mintier+1)*3;
//        System.out.println(blocknum);

        for (var i = 0; i < blocknum; i++) {
            Block newblock = new Block.Normal();
            newblock.type = 0;
            newblock.isVisible = true;
            newblock.isDestructible = true;
            newblock.isInteractive = true;
            block.add(newblock);
        }
        info.blocks = block.toArray(new Block[0]);

//        System.out.println(info.blocks.length);

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
        StarInfo.gen(0);
//        Position pos = StarInfo.posOf(43);
//        System.out.print("angle1:\t");
//        System.out.println(StarInfo.angleOf(43));
//        System.out.print("tier:\t");
//        System.out.println(StarInfo.tierOf(43));
//        System.out.printf(String.format("%g, %g, \nangle2:\t%g", pos.x, pos.y, Math.atan(pos.y/pos.x)));
    }

    public static int realINdexOf(int index, int mintier) {
        return 3*mintier*(mintier+1) + index + 1;
    }

    public static int tierOf(int realindex) {
        return (int)(0.5 + Math.pow((12*realindex+9), 1.0/2)/6.0);
    }

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
//    public static double angleOf(int realindex) {
//        int tier = tierOf(realindex);
//        int reltindex = realindex - 3*tier*(tier-1) - 1;
//        double i = (reltindex%tier)/(double)tier;
//        double x = 1 - i * Math.cos(Math.PI/3.0);
//        double y = i * Math.sin(Math.PI/3.0);
//        return Math.atan(y/x) + Math.PI*(int)(reltindex/tier)/3.0;
//    }
}
