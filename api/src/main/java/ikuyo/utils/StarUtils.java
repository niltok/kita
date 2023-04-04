package ikuyo.utils;

import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import org.dyn4j.collision.narrowphase.Gjk;
import org.dyn4j.collision.narrowphase.Raycast;
import org.dyn4j.collision.narrowphase.RaycastDetector;
import org.dyn4j.geometry.*;

import java.util.ArrayList;
import java.util.List;

public class StarUtils {
    public static final int areaTier = 30;
    public static final int areaNum = areaTier * (areaTier + 1) * 3 + 1;
    public static final int areaSize = 15;
    public static final int blockRealNum = StarInfo.maxTier * (StarInfo.maxTier + 1) * 3 + 1;
    public static final int blockNum = blockRealNum - StarInfo.minTier * (StarInfo.minTier - 1) * 3 - 1;

    public static int realIndexOf(int index, int mintier) {
        return 3 * mintier * (mintier - 1) + index + 1;
    }

    public static int tierOf(int realIndex) {
        double tier = 0.5 + Math.sqrt(12 * realIndex + 9) / 6.0;
        return (int) Math.ceil(tier) - 1;
    }

    /**<p> 单位 1 : 根3边长;  两层距离 : 二分之根3 <p/>*/
    public static Position posOf(int realIndex) {
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
        Position pos = posOf(realIndex);
        return Math.hypot(pos.x, pos.y);
    }

    /**
     * return angle in the range of 0 to 2pi.
     */
    public static double angleOf(int realIndex) {
        Position pos = posOf(realIndex);
        return (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2);
    }

    public static boolean isStandable(double x, double y, double r, StarInfo star) {
        boolean res = true;
        if (Math.hypot(x, y) < StarInfo.maxTier * StarInfo.tierDistance) {
            int index = realIndexOf(x, y);
//            System.out.println("[index]: %d".formatted(index));
            Position pos = posOf(index);
            double rr = Math.hypot(pos.x - x, pos.y - y) + r;
            int tiers = (int) (rr / StarInfo.tierDistance) + 1;

            int[] blocklist = nTierAround(index, tiers)
                    .stream().mapToInt(Integer::valueOf).toArray();

//            System.out.println("[r]: %f, [rr]: %f, [tier]: %d, [blocks]: %d".formatted(r, rr, tiers, blocklist.length));
//            for (var i : blocklist) { System.out.println(i); }

            for (var i : blocklist) {
                if (star.blocks[i].isCollisible) {
                    Position posI = posOf(i);
                    if (Math.hypot(posI.x - x, posI.y - y) < r) res = false;
                    break;
                }
            }
        }
        return res;
    }

    public static int indexOf(int realIndex, int minTier) {
        return realIndex - minTier * (minTier - 1) * 3 - 1;
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
        double roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e9) / 1e9;

        int detectIndex = edge * tier
                + (int) (roundPercent * tier)
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
                radian + Math.PI), 1.5, hexagon, transform, raycast);

        int index = detectIndex;
        if (raycast.getPoint().getMagnitude() < Math.hypot(x, y)) {
            tier++;
            roundPercent = Math.round((percent + (tier == 0 ? 0 : 1.0 / tier / 2)) * 1e9) / 1e9;
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
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex
     */
    public static ArrayList<Integer> nTierAround(int realIndex, int n) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realIndex);
        int extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3;
        for (int i = 0; i < n * (n + 1) * 3; i++) {
            Position posI = posOf(i + 1);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);
            if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier) res.add(index - extraBlock - 1);
        }
        return res;
    }

    public static ArrayList<Integer> nTierAround(Position position, double r) {
        ArrayList<Integer> res = new ArrayList<>();
        Position pos = posOf(realIndexOf(position.x, position.y));
        int extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3;
        int nTier = (int) ((r + StarInfo.edgeLength) / StarInfo.tierDistance) + 1;
        for (int i = 0; i < nTier * (nTier + 1) * 3 + 1; i++) {
            Position posI = posOf(i);
            int index = realIndexOf(pos.x + posI.x, pos.y + posI.y);
            int tier = tierOf(index);
            if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier && position.distance(posOf(index)) <= r)
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

            if (tier >= StarInfo.minTier && tier <= StarInfo.maxTier) {
                if (star.blocks[indexOf(index, StarInfo.minTier)].isVisible) {
                    int[] blocklist = nTierAround(index, 1)
                            .stream().mapToInt(Integer::valueOf).toArray();
                    for (var b : blocklist) {
                        if (star.blocks[b].type == 0) {
                            result.add(indexOf(index, StarInfo.minTier));
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
        Position pos = posOf(realIndex);
        Vector2 trans = new Vector2(pos.x, pos.y);
        trans.rotate(Math.PI / 6).divide(StarInfo.tierDistance * tier);

        return realIndexOf(trans.x, trans.y);
    }

    /**
     * <p>area内块的编号<p/>
     * 返回的List中存储可以直接调用的 index , 而不是 realIndex
     */
    public static ArrayList<Integer> getBlocksAt(int area, int tier) {
        Position center = posOf(area);
        Vector2 trans = new Vector2(center.x, center.y);
        trans.rotate(-Math.PI / 6).multiply(StarInfo.tierDistance * tier);
        int centerIndex = realIndexOf(trans.x, trans.y);
        ArrayList<Integer> list = nTierAround(centerIndex, tier - 1);

        int extraBlock = StarInfo.minTier * (StarInfo.minTier - 1) * 3;
        for (int index = tier * (tier - 1) * 3 + 1; index < tier * (tier + 1) * 3 + 1; index++) {
            int thisTier = tierOf(index);
            if (thisTier >= StarInfo.minTier && thisTier <= StarInfo.maxTier && getAreaOf(index, tier) == area)
                list.add(index - extraBlock - 1);
        }

        return list;
    }

    public static ArrayList<Integer> areasAround(double x, double y, double r, int tier) {
        Vector2 trans = new Vector2(x, y);
        trans.rotate(Math.PI / 6).divide(StarInfo.tierDistance * tier);
        return nTierAround(new Position(trans.x, trans.y), r / StarInfo.tierDistance / tier + StarInfo.edgeLength);
    }

    public static List<Integer> getBlocksAt(int area) {
        return getBlocksAt(area, areaSize);
    }

    public static int getAreaOf(int realIndex) {
        return getAreaOf(realIndex, areaSize);
    }

    public static List<Integer> areasAround(double x, double y, double r) {
        return areasAround(x, y, r, areaSize);
    }

    public static void main(String[] args) {

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

    }
}
