package ikuyo.utils;

import ikuyo.api.datatypes.StarInfo;
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
    public static final int insideBlocks = StarInfo.minTier * (StarInfo.minTier - 1) * 3 + 1;
    public static final int blockRealNum = StarInfo.maxTier * (StarInfo.maxTier + 1) * 3 + 1;
    public static final int blockNum = blockRealNum - insideBlocks;
    public static int realIndexOf(int index) {
        return insideBlocks + index;
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

    public static Vector2 vPositionOf(int realIndex) {
        Position position = positionOf(realIndex);
        return new Vector2(position.x, position.y);
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
        return realIndex - insideBlocks;
    }

    private static final Polygon hexagon = Geometry.createPolygon(getVertices());
    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(StarInfo.edgeLength + 1e-6, 0)
                    .rotate(Math.PI / 3 * i + Math.PI / 6);
        }
        return vertices;
    }
    public static int realIndexOf(double x, double y) {
        double radian = Math.atan2(y, x);
        if (radian < 0) radian += Math.PI * 2;
        if (radian >= Math.PI * 2) radian -= Math.PI * 2;

        double PI_DIV3 = Math.PI / 3;
        int edge = (int) (Math.round((radian / PI_DIV3) * 1e8) / 1e8);
        double i = radian - PI_DIV3 * edge;
        if (i <= 1e-8) {
            edge--;
            i = Math.min(1 + PI_DIV3, PI_DIV3);
        }
        edge = (edge + 6) % 6;

        double height = Math.hypot(x, y) * Math.cos(Math.abs(Math.PI / 6 - i));
        int tier = (int) (height / StarInfo.tierDistance);
        double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
        int detectIndex = getIndex(i, edge, height, tier, percent);
        Position pos = positionOf(detectIndex);

        RaycastDetector raycastDetector = new Gjk();
        Transform transform = new Transform();
        transform.setTranslation(pos.x, pos.y);
        Raycast raycast = new Raycast();

        raycastDetector.raycast(new Ray(new Vector2(x, y).add(new Vector2(radian).multiply(2)),
                radian + Math.PI), 4, hexagon, transform, raycast);

        int index = detectIndex;
        if (raycast.getPoint().getMagnitude() < Math.hypot(x, y))
            index = getIndex(i, edge, height, ++tier, percent);

        return index;
    }

    private static int getIndex(double i, int edge, double height, int tier, double percent) {
        double roundPercent = percent
                + (Math.tan(i - Math.PI / 6) * (height - tier * StarInfo.tierDistance) / tier)
                + (tier == 0 ? 0 : 1.0 / tier / 2);
        roundPercent  = Math.ceil(roundPercent * 1e8) / 1e8;
        int index = edge * tier
                + (int)(roundPercent * tier)
                + (tier - 1) * tier * 3
                + Math.min(tier, 1);
        if (tier != 0 && index / ((tier) * (tier + 1) * 3 + 1) >= 1)
            index = (tier) * (tier - 1) * 3 + 1;

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
            extraBlock = insideBlocks;
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
            extraBlock = insideBlocks;
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
    public static int getAreaOf(int realIndex, int areaSize) {
        Position pos = positionOf(realIndex);
        Vector2 trans = blockToArea(pos.x, pos.y, areaSize);

        return realIndexOf(trans.x, trans.y);
    }

    /**
     * <p>area内块的编号<p/>
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex
     */
    public static ArrayList<Integer> getBlocksAt(int area, int areaSize) {
        Position center = positionOf(area);
        Vector2 trans = areaToBlock(center.x, center.y, areaSize);
        ArrayList<Integer> list =
                nTierAround(realIndexOf(trans.x, trans.y), areaSize - 1, false);

        for (int i = areaSize * (areaSize - 1) * 3 + 1; i < areaSize * (areaSize + 1) * 3 + 1; i++) {
            Position posI = positionOf(i);
            int realIndexI = realIndexOf(trans.x + posI.x, trans.y + posI.y);
            int tier = tierOf(realIndexI);
            if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier && getAreaOf(realIndexI, areaSize) == area)
                list.add(indexOf(realIndexI));
        }

        return list;
    }

    public static ArrayList<Integer> areasAround(double x, double y, double r, int areaSize) {
        Vector2 trans = blockToArea(x, y, areaSize);
        ArrayList<Integer> list = nTierAround(new Position(trans.x, trans.y),
                r / (StarInfo.tierDistance * areaSize * 2) + StarInfo.edgeLength, true);
        ArrayList<Integer> res = new ArrayList<>();
        for (var i: list) {
            if (i < areaSize)
                res.add(i);
        }

        return res;
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

    public static Vector2 areaToBlock(double x ,double y, double areaSize) {
        Vector2 trans = new Vector2(x, y);
        trans.multiply(StarInfo.tierDistance * areaSize * 2).rotate(Math.PI / 6);
        return trans;
    }

    public static Vector2 blockToArea(double x ,double y, double areaSize) {
        Vector2 trans = new Vector2(x, y);
        trans.inverseRotate(Math.PI / 6).divide(StarInfo.tierDistance * areaSize * 2);
        return trans;
    }

    public static String printBlock(int realIndex) {
        Position position = positionOf(realIndex);

        double radian = Math.atan2(position.y, position.x);
        if (radian < 0) radian += Math.PI * 2;
        if (radian >= Math.PI * 2) radian -= Math.PI * 2;

        double PI_DIV3 = Math.PI / 3;
        int edge = (int) (Math.round((radian / PI_DIV3) * 1e8) / 1e8);
        double i = radian - PI_DIV3 * edge;
        if (i <= 1e-8) {
            edge--;
            i = Math.min(1 + PI_DIV3, PI_DIV3);
        }
        edge = (edge + 6) % 6;

        double height = Math.hypot(position.x, position.y) * Math.cos(Math.abs(Math.PI / 6 - i));
        int tier = (int) (height / StarInfo.tierDistance);
        double percent = 2 / (Math.sqrt(3) / Math.tan(i) + 1);
        double roundPercent = percent
                + (Math.tan(i - Math.PI / 6) * (height - tier * StarInfo.tierDistance) / tier)
                + (tier == 0 ? 0 : 1.0 / tier / 2);

        StringBuilder around = new StringBuilder("[aroundBlocks]:");
        for (int index = 1; index < 7; index++) {
            Position posI = positionOf(index);
            around.append(" {%d}".formatted(realIndexOf(position.x + posI.x, position.y + posI.y)));
        }

        return ("""
                [info - %d]: {\s
                \t[realIndex]: %d, [index]: %d, [tier]: %d, [position]: (x) %f, (y) %f\s
                \t[angle]: %f, [edge]: %d, [Percent]: %f, [roundPercent], %f\s
                \t[area]: %d, %s\s
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
                        StarUtils.getAreaOf(realIndex),
                        around.toString());
    }

    public static void main(String[] args) {

/*
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
*/

        Position center = positionOf(16);
        Vector2 trans = new Vector2(center.x, center.y);
        trans.multiply(StarInfo.tierDistance * areaSize * 2).rotate(Math.PI / 6);
        ArrayList<Integer> list =
                nTierAround(realIndexOf(trans.x, trans.y), areaSize - 1, true);

    }
}