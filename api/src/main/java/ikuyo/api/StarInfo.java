package ikuyo.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.equipments.AbstractWeapon;
import ikuyo.api.spaceships.AbstractSpaceship;
import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.dyn4j.collision.narrowphase.Gjk;
import org.dyn4j.collision.narrowphase.Raycast;
import org.dyn4j.collision.narrowphase.RaycastDetector;
import org.dyn4j.geometry.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

        //地面最低层数
        int tierNum = (int)(random.nextDouble()*(maxTier - minTier) * 0.5
                + (maxTier - minTier) * 0.25)
                + minTier;
        //地基层数
        int baseTier = Math.min((int)(maxTier * 0.1), 100);
        //生成噪声参数
        double noiseLength = 30.0;

        tierNum = Math.max(maxTier - baseTier - (int)(random.nextDouble() * 100) - 100, tierNum);

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
        public double san = 100;
        public AbstractSpaceship spaceship = new AbstractSpaceship(CargoStatic.shuttle.type());
        public String controlType = "walk";
        public StarUserInfo() {
            spaceship.weapons.add(new AbstractWeapon(CargoStatic.defaultWeapon.type()));
            spaceship.weapons.add(new AbstractWeapon(CargoStatic.r400.type()));
            spaceship.cargoHold.put(CargoStatic.defaultAmmo.type(), 500);
        }
        public StarUserInfo(double x, double y) {
            this();
            this.x = x;
            this.y = y;
            online = true;
        }
    }

    public static void main(String[] args) {
/*
        for (int i = 0; i < 100; i++) {
            System.out.println(OpenSimplex2S.noise2(0, i, 0) + 1);
        }
        System.out.println(OpenSimplex2S.noise2(0, 6, 0) + 1);
        System.out.println(Math.tan(0.8 * Math.PI / 2));
*/

/*
        int testTier = 1000;
        int error = 0;
        for (int index = 0; index < testTier * (testTier+1) * 3 + 1; index++) {
            Position position = posOf(index);
            double x = position.x, y = position.y;

            double radian = (Math.atan2(y, x) + Math.PI * 2) % (Math.PI * 2);
            double PIDiv3 = Math.PI / 3;
            int edge = (int)(radian / PIDiv3);
            double i = radian % PIDiv3;
            int tier = (int)(Math.cos(Math.abs(Math.PI / 6 - i))
                    * Math.hypot(x, y) / tierDistance);
            double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
            percent += tier == 0 ? 0 : 1.0 / tier / 2;
            percent = Math.round(percent * 1e6) / 1e6;

            int detectIndex = edge * tier
                    + (int)(percent * tier)
                    + (tier - 1) * tier * 3 + Math.min(tier, 1);
            if (detectIndex != index)
                error++;
            System.out.print(index + ", " + detectIndex + ", " + percent + "\n");
        }
        System.out.println(error);
*/


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

    //    todo: new Real
    private static final Polygon hexagon = Geometry.createPolygon(getVertices());
    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(StarInfo.edgeLength * 1.001, 0).rotate(Math.PI / 3 * i + Math.PI / 6);
        }
        return vertices;
    }
    public static int realIndexOf(double x, double y) {
        double radian = (Math.atan2(y, x) + Math.PI * 2) % (Math.PI * 2);
        double PIDiv3 = Math.PI / 3;
        int edge = (int)(radian / PIDiv3);
        double i = radian % PIDiv3;
        int tier = (int)(Math.cos(Math.abs(Math.PI / 6 - i))
                * Math.hypot(x, y) / tierDistance);
        double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
        double roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e9) / 1e9;

        int detectIndex = edge * tier
                + (int)(roundPercent * tier)
                + (tier - 1) * tier * 3 + Math.min(tier, 1);
        if (tier != 0 && detectIndex / ((tier) * (tier + 1) * 3 + 1) >= 1)
            detectIndex = (tier) * (tier - 1) * 3 + 1;
        Position pos = posOf(detectIndex);

        RaycastDetector raycastDetector = new Gjk();
        Transform transform = new Transform();
        transform.setTranslation(pos.x, pos.y);
        Raycast raycast = new Raycast();

        raycastDetector.raycast(new Ray(new Vector2(), radian), Math.hypot(x, y), hexagon, transform, raycast);
        raycastDetector.raycast(new Ray(raycast.getPoint().add(new Vector2(radian).multiply(1.5)),
                radian + Math.PI),1.5, hexagon, transform, raycast);

        int index = detectIndex;
        if (raycast.getPoint().getMagnitude() < Math.hypot(x, y)) {
            tier++;
            roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e9) / 1e9;
            index = edge * tier
                    + (int)(roundPercent * tier)
                    + (tier - 1) * tier * 3 + Math.min(tier, 1);
            if (index / ((tier) * (tier + 1) * 3 + 1) >= 1)
                index = (tier) * (tier - 1) * 3 + 1;
        }

        return index;
    }

/** <p>realIndex 周围 n 层的块的编号<p/>
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

    /** <p>查询 realIndex 所属区域的编号<p/>*/
    public static int getAreaOf(int realIndex, int tier) {
        Position pos = posOf(realIndex);
        Vector2 trans = new Vector2(pos.x, pos.y);
        trans.rotate(Math.PI / 6).divide(tierDistance * tier);

        return realIndexOf(trans.x, trans.y);
    }

    /** <p>area内块的编号<p/>
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex*/
    public static ArrayList<Integer> getBlocksAt(int area, int tier) {
        Position center = posOf(area);
        Vector2 trans = new Vector2(center.x, center.y);
        trans.rotate(-Math.PI / 6).multiply(tierDistance * tier);
        int centerIndex = realIndexOf(trans.x, trans.y);
        ArrayList<Integer> list = nTierAround(centerIndex, tier - 1);

        int extraBlock = minTier * (minTier - 1) * 3;
        for (int index = tier * (tier - 1) * 3 + 1; index < tier * (tier + 1) * 3 + 1; index++) {
            int thisTier = tierOf(index);
            if (thisTier >= minTier && thisTier <= maxTier && getAreaOf(index, tier) == area)
                list.add(index - extraBlock - 1);
        }

        return list;
    }

    public static ArrayList<Integer> areasAround(double x, double y, double r, int tier) {
        Vector2 trans = new Vector2(x, y);
        trans.rotate(-Math.PI / 6).multiply(tierDistance * tier);
        return nTierAround(new Position(trans.x, trans.y), r / tierDistance / tier + edgeLength);
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