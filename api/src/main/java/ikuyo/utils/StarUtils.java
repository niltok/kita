package ikuyo.utils;

import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import org.dyn4j.collision.narrowphase.Gjk;
import org.dyn4j.collision.narrowphase.Raycast;
import org.dyn4j.collision.narrowphase.RaycastDetector;
import org.dyn4j.geometry.*;

import java.util.ArrayList;
import java.util.List;

public final class StarUtils {
    public static final int areaTier = 30;
    public static final int areaNum = areaTier * (areaTier + 1) * 3 + 1;
    public static final int areaSize = 15;
    public static final int blockRealNum = StarInfo.maxTier * (StarInfo.maxTier + 1) * 3 + 1;
    public static final int blockNum = blockRealNum - StarInfo.minTier * (StarInfo.minTier - 1) * 3 - 1;

    public static int realIndexOf(int index) {
        return 3 * StarInfo.minTier * (StarInfo.minTier - 1) + index + 1;
    }

    public static int tierOf(int realIndex) {
        double tier = 0.5 + Math.sqrt(12 * realIndex + 9) / 6.0;
        return (int) Math.ceil(tier) - 1;
    }

    /**<p> 单位 1 : 根3边长;  两层距离 : 二分之根3 <p/>*/
    public static Position positionOf(int realIndex) {
        if (realIndex == 0) return new Position(0, 0);
        Position pos = new Position();
        int tier = tierOf(realIndex);
        int relativeIndex = realIndex - 3 * tier * (tier - 1) - 1;
        double i = (relativeIndex % tier) / (double) tier;
        double x = 1 - i * Math.cos(Math.PI / 3.0);
        double y = i * Math.sin(Math.PI / 3.0);
        double l = Math.hypot(x, y);
        double angle = Math.atan(y / x) + Math.PI * (double) (relativeIndex / tier) / 3.0;
        pos.x = Math.cos(angle) * l * tier;
        pos.y = Math.sin(angle) * l * tier;
        return pos;
    }

    public static double heightOf(int realIndex) {
        Position pos = positionOf(realIndex);
        return Math.hypot(pos.x, pos.y);
    }

    /**
     * return angle in the range of 0 to 2pi.
     */
    public static double angleOf(int realIndex) {
        Position pos = positionOf(realIndex);
        return (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2);
    }

    public static boolean isStandable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        if (Math.hypot(x, y) < StarInfo.maxTier * StarInfo.tierDistance) {
            int index = realIndexOf(x, y);
//            System.out.println("[index]: %d".formatted(index));
            Position pos = positionOf(index);
            double rr = Math.hypot(pos.x - x, pos.y - y) + r;
            int tiers = (int) (rr / StarInfo.tierDistance) + 1;

            int[] blocklist = nTierAround(index, tiers, false)
                    .stream().mapToInt(Integer::valueOf).toArray();

//            System.out.println("[r]: %f, [rr]: %f, [tier]: %d, [blocks]: %d".formatted(r, rr, tiers, blocklist.length));
//            for (var i : blocklist) { System.out.println(i); }

            for (var i : blocklist) {
                if (star.blocks[i].isCollisible) {
                    Position posI = positionOf(i);
                    if (Math.hypot(posI.x - x, posI.y - y) < r) res = false;
                    break;
                }
            }
        }
        return res;
    }

    public static int indexOf(int realIndex) {
        return realIndex - StarInfo.minTier * (StarInfo.minTier - 1) * 3 - 1;
    }

    private static final Polygon hexagon = Geometry.createPolygon(getVertices());
    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(StarInfo.edgeLength, 0).rotate(Math.PI / 3 * i + Math.PI / 6);
        }
        return vertices;
    }
    public static int realIndexOf(double x, double y) {
        double radian = (Math.atan2(y, x) + Math.PI * 2) % (Math.PI * 2);
        double PIDiv3 = Math.PI / 3;
        int edge = (int) (radian / PIDiv3);
        double i = radian % PIDiv3;
        int tier = (int) (Math.cos(Math.abs(Math.PI / 6 - i))
                * Math.hypot(x, y) / StarInfo.tierDistance);
        double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
        double roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e8) / 1e8;

        int detectIndex = edge * tier
                + (int) (roundPercent * tier)
                + (tier - 1) * tier * 3 + Math.min(tier, 1);
        if (tier != 0 && detectIndex / ((tier) * (tier + 1) * 3 + 1) >= 1)
            detectIndex = (tier) * (tier - 1) * 3 + 1;
        Position pos = positionOf(detectIndex);

        RaycastDetector raycastDetector = new Gjk();
        Transform transform = new Transform();
        transform.setTranslation(pos.x, pos.y);
        Raycast raycast = new Raycast();

        raycastDetector.raycast(new Ray(new Vector2(), radian), Math.hypot(x, y), hexagon, transform, raycast);
        raycastDetector.raycast(new Ray(raycast.getPoint().add(new Vector2(radian).multiply(1.5)),
                radian + Math.PI), 1.5, hexagon, transform, raycast);

        int index = detectIndex;
        if (raycast.getPoint().getMagnitude() < Math.hypot(x, y)) {
            tier++;
            roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e8) / 1e8;
            index = edge * tier
                    + (int) (roundPercent * tier)
                    + (tier - 1) * tier * 3 + Math.min(tier, 1);
            if (index / ((tier) * (tier + 1) * 3 + 1) >= 1)
                index = (tier) * (tier - 1) * 3 + 1;
        }

        return index;
    }

    /**
     * <p>realIndex 周围 n 层的块的编号<p/>
     * @param realIndex 被查询块的真实编号
     * @param n 查询层数
     * @param returnRealIndex 是否返回真实编号<br/> <p> false 时会返回可以直接调用的 index 并进行过滤<p/>
     */
    public static ArrayList<Integer> nTierAround(int realIndex, int n, boolean returnRealIndex) {
        ArrayList<Integer> res = new ArrayList<>();
        if (n < 0) return res;
        Position pos = positionOf(realIndex);
        int extraBlock = 0;
        if (!returnRealIndex)
            extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3 + 1;
        for (int i = 0; i < n * (n + 1) * 3 + 1; i++) {
            Position posI = positionOf(i);
            int realIndexI = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            if (returnRealIndex) res.add(realIndex - extraBlock);
            else {
                int tier = tierOf(realIndexI);
                if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier)
                    res.add(realIndexI - extraBlock);
            }
        }
        return res;
    }

    /**
     * <p>position 周围 r 范围内的块的编号<p/>
     * @param position 中心位置
     * @param r 覆盖半径
     * @param returnRealIndex 是否返回真实编号<br/> <p> false 时会返回可以直接调用的 index 并进行过滤<p/>
     */
    public static ArrayList<Integer> nTierAround(Position position, double r, boolean returnRealIndex) {
        ArrayList<Integer> res = new ArrayList<>();
        if (r < 0) return res;
        Position pos = positionOf(realIndexOf(position.x, position.y));
        int extraBlock = 0;
        if (!returnRealIndex)
            extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3 + 1;
        int nTier = (int) ((r + StarInfo.edgeLength) / StarInfo.tierDistance) + 1;
        for (int i = 0; i < nTier * (nTier + 1) * 3 + 1; i++) {
            Position posI = positionOf(i);
            int realIndex = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            if (returnRealIndex)
                if (position.distance(positionOf(realIndex)) <= r)
                    res.add(realIndex - extraBlock);
            else {
                int tier = tierOf(realIndex);
                if (tier >= StarInfo.minTier
                        && tier <= StarInfo.maxTier
                        && position.distance(positionOf(realIndex)) <= r)
                    res.add(realIndex - extraBlock);
            }
        }
        return res;
    }

    public static ArrayList<Integer> surfaceBlocks(int realIndex, int startTier, int endTier, StarInfo star) {
        ArrayList<Integer> result = new ArrayList<>();
        Position pos = positionOf(realIndex);

        for (int i = (startTier <= 0 ? 0 : startTier * (startTier - 1) * 3 + 1);
             i < endTier * (endTier + 1) * 3 + 1; i++) {
            Position posI = positionOf(i);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);

            if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier) {
                if (star.blocks[indexOf(index)].isVisible) {
                    ArrayList<Integer> list = nTierAround(index, 1, false);
                    if (!list.isEmpty()) list.remove(0);
                    for (var b : list) {
                        if (star.blocks[b].type == 0) {
                            result.add(indexOf(index));
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * <p>查询 realIndex 所属区域的编号<p/>
     */
    public static int getAreaOf(int realIndex, int tier) {
        Position pos = positionOf(realIndex);
        Vector2 trans = new Vector2(pos.x, pos.y);
        trans.inverseRotate(Math.PI / 6).divide(StarInfo.tierDistance * tier * 2);

        return realIndexOf(trans.x, trans.y);
    }

    /**
     * <p>area内块的编号<p/>
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex
     */
    public static ArrayList<Integer> getBlocksAt(int area, int tier) {
        Position center = positionOf(area);
        Vector2 trans = new Vector2(center.x, center.y);
        trans.rotate(Math.PI / 6).multiply(StarInfo.tierDistance * tier * 2);
        ArrayList<Integer> list =
                nTierAround(realIndexOf(trans.x, trans.y), tier - 1, false);

        int extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3 + 1;
        for (int i = tier * (tier - 1) * 3 + 1; i < tier * (tier + 1) * 3 + 1; i++) {
            Position posI = positionOf(i);
            int realIndexI = realIndexOf(trans.x + posI.x, trans.y + posI.y);
            int thisTier = tierOf(realIndexI);
            if (thisTier >= StarInfo.minTier && thisTier <= StarInfo.maxTier && getAreaOf(realIndexI, tier) == area)
                list.add(realIndexI - extraBlock);
        }

        return list;
    }

    public static ArrayList<Integer> areasAround(double x, double y, double r, int tier) {
        Vector2 trans = new Vector2(x, y);
        trans.inverseRotate(Math.PI / 6).divide(StarInfo.tierDistance * tier * 2);
        return nTierAround(new Position(trans.x, trans.y), r / (StarInfo.tierDistance * tier * 2) + StarInfo.edgeLength, true);
    }

    /**
     * <p>area内块的编号<p/>
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex
     */
    public static List<Integer> getBlocksAt(int area) {
        return getBlocksAt(area, areaSize);
    }

    public static int getAreaOf(int realIndex) {
        return getAreaOf(realIndex, areaSize);
    }

    public static List<Integer> areasAround(double x, double y, double r) {
        return areasAround(x, y, r, areaSize);
    }

    public static String printBlock(int realIndex) {
        Position position = positionOf(realIndex);

        double radian = (Math.atan2(position.y, position.x) + Math.PI * 2) % (Math.PI * 2);
        double PIDiv3 = Math.PI / 3;
        int edge = (int) (radian / PIDiv3);
        double i = radian % PIDiv3;
        int tier = (int) (Math.cos(Math.abs(Math.PI / 6 - i))
                * Math.hypot(position.x, position.y) / StarInfo.tierDistance);
        double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
        double roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e8) / 1e8;

        StringBuilder around = new StringBuilder("[aroundBlocks]:");
        for (int index = 1; index < 7; index++) {
            Position posI = positionOf(index);
            around.append(" {%d}".formatted(realIndexOf(position.x + posI.x, position.y + posI.y)));
        }

        return ("""
                [info - %d]: {\s
                \t[realIndex]: %d, [index]: %d, [tier]: %d, [position]: (x) %f, (y) %f\s
                \t[angle]: %f, [edge]: %d, [edgePercent]: %f, [roundPercent], %f\s
                \t%s\s
                }""")
                .formatted(realIndex,
                        realIndex,
                        indexOf(realIndex),
                        tierOf(realIndex),
                        position.x, position.y,
                        radian,
                        edge + 1,
                        percent,
                        roundPercent,
                        around.toString());
    }

    public static void main(String[] args) {

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
                    * Math.hypot(x, y) / StarInfo.tierDistance);
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

        Position pos = positionOf(2);
        RaycastDetector raycastDetector = new Gjk();
        Transform transform = new Transform();
        transform.setTranslation(pos.x, pos.y);
        Raycast raycast = new Raycast();

        raycastDetector.raycast(
                new Ray(new Vector2(Math.PI / 2).getNormalized().multiply(5), -Math.PI / 2),
                10, hexagon, transform, raycast);

        System.out.println(raycast.getPoint());
        System.out.println(StarInfo.edgeLength * 2);
    }
}