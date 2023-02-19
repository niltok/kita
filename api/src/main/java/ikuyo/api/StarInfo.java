package ikuyo.api;

import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

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
        return toJson().toString();
    }

    public Buffer toBuffer() {
        return DataStatic.gzipEncode(toJson().toBuffer());
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

//        纯天空生成
        for (var i = groundnum; i < blocknum; i++) {
            info.blocks[i].type = 0;
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
        return fromJson(new JsonObject(DataStatic.gzipDecode(buffer)));
    }

    public static StarInfo fromJson(JsonObject json) {
        return json.mapTo(StarInfo.class);
    }

    public static class StarUserInfo {
        public double x, y = 6000;
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
}

//class OpenSimplexNoise {
//
//    private OpenSimplexNoiseKS generator;
//
//    public final static String VERSION = "##library.prettyVersion##";
//
//
//    /**
//     * Constructs a new OpenSimplexNoise object,
//     * using the system's current time as the noise seed.
//     */
//    public OpenSimplexNoise() {
//        this(System.currentTimeMillis());
//    }
//
//    /**
//     * Constructs a new OpenSimplexNoise object,
//     * using the provided value as the noise seed.
//     */
//    public OpenSimplexNoise(long seed) {
//        welcome();
//        generator = new OpenSimplexNoiseKS(seed);
//    }
//
//
//    private void welcome() {
//        System.out.println("##library.name## ##library.prettyVersion## by ##author##");
//    }
//
//    private double remap(double val) {
//        return (val + 1) * 0.5;
//    }
//
//
//    public float noise (float xoff) {
//        return this.noise(xoff, 0);
//    }
//
//    public float noise (float xoff, float yoff) {
//        return (float) remap(generator.eval(xoff, yoff));
//    }
//
//    public float noise (float xoff, float yoff, float zoff) {
//        return (float) remap(generator.eval(xoff, yoff, zoff));
//    }
//
//    public float noise (float xoff, float yoff, float zoff, float uoff) {
//        return (float) remap(generator.eval(xoff, yoff, zoff, uoff));
//    }
//
//
//    /**
//     * return the version of the Library.
//     *
//     * @return String
//     */
//    public static String version() {
//        return VERSION;
//    }
//}
//
////final class ImprovedNoise {
////    static public double noise(double x, double y, double z) {
////        int X = (int)Math.floor(x) & 255,                  // FIND UNIT CUBE THAT
////                Y = (int)Math.floor(y) & 255,                  // CONTAINS POINT.
////                Z = (int)Math.floor(z) & 255;
////        x -= Math.floor(x);                                // FIND RELATIVE X,Y,Z
////        y -= Math.floor(y);                                // OF POINT IN CUBE.
////        z -= Math.floor(z);
////        double u = fade(x),                                // COMPUTE FADE CURVES
////                v = fade(y),                                // FOR EACH OF X,Y,Z.
////                w = fade(z);
////        int A = p[X  ]+Y, AA = p[A]+Z, AB = p[A+1]+Z,      // HASH COORDINATES OF
////                B = p[X+1]+Y, BA = p[B]+Z, BB = p[B+1]+Z;      // THE 8 CUBE CORNERS,
////
////        return lerp(w, lerp(v, lerp(u, grad(p[AA  ], x  , y  , z   ),  // AND ADD
////                                grad(p[BA  ], x-1, y  , z   )), // BLENDED
////                        lerp(u, grad(p[AB  ], x  , y-1, z   ),  // RESULTS
////                                grad(p[BB  ], x-1, y-1, z   ))),// FROM  8
////                lerp(v, lerp(u, grad(p[AA+1], x  , y  , z-1 ),  // CORNERS
////                                grad(p[BA+1], x-1, y  , z-1 )), // OF CUBE
////                        lerp(u, grad(p[AB+1], x  , y-1, z-1 ),
////                                grad(p[BB+1], x-1, y-1, z-1 ))));
////    }
////    static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
////    static double lerp(double t, double a, double b) { return a + t * (b - a); }
////    static double grad(int hash, double x, double y, double z) {
////        int h = hash & 15;                      // CONVERT LO 4 BITS OF HASH CODE
////        double u = h<8 ? x : y,                 // INTO 12 GRADIENT DIRECTIONS.
////                v = h<4 ? y : h==12||h==14 ? x : z;
////        return ((h&1) == 0 ? u : -u) + ((h&2) == 0 ? v : -v);
////    }
////    static final int[] p = new int[512], permutation = { 151,160,137,91,90,15,
////            131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
////            190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
////            88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
////            77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
////            102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
////            135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
////            5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
////            223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
////            129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
////            251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
////            49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
////            138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
////    };
////    static { for (int i=0; i < 256 ; i++) p[256+i] = p[i] = permutation[i]; }
////}