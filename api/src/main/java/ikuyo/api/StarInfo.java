package ikuyo.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.utils.DataStatic;
import ikuyo.utils.StarUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StarInfo {
    /**层级最大值*/
    public static final int maxTier = 2 * StarUtils.areaSize * StarUtils.areaTier;
    /**<p>层级最小值<p/>
     * [Warn]: Plz make sure mintier > 0*/
    public static final int minTier = 10;
    /**层级间距*/
    public static final double tierDistance = Math.sqrt(3)/2;
    /**六边形块边长*/
    public static final double edgeLength = 1 / Math.sqrt(3);
    /**星球半径*/
    protected double star_r;
    public Block[] blocks;
    public Map<Integer, UserInfo> starUsers;

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
    public static StarInfo generate(int seed) {
        var info = new StarInfo();
        Random random = new Random(seed);
        info.blocks = new Block[StarUtils.blockNum];
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

 //        圆角修饰部分_in
        int indexInside = StarUtils.realIndexOf(0, minTier);
        double rInside = StarUtils.heightOf(indexInside);
        int tierInside = (int)((Math.sqrt(3)*2/3-1) * rInside) + 1 + minTier;
        int roundNumInside = tierInside*(tierInside+1)*3 - minTier *(minTier -1)*3;
        if (roundNumInside > StarUtils.blockNum) roundNumInside = StarUtils.blockNum;
        for (var i = 0; i < roundNumInside; i++) {
            if ( StarUtils.heightOf(indexInside) < rInside ) info.blocks[i].type = 0;
            else {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
            }
            indexInside++;
        }

//        计算地表
        int roundStartTier = (int)(tierNum * tierDistance) - 1;
        int groundNum =  (tierNum + baseTier) * ((tierNum+baseTier)+1) * 3 - minTier * (minTier -1) * 3;
        int roundNumOutside = roundStartTier*(roundStartTier+1)*3 - minTier *(minTier -1)*3;
        if (roundNumOutside < 0) roundNumOutside = 0;

//        纯地面生成
        for (var i = roundNumInside; i < roundNumOutside; i++) {
            info.blocks[i].type = 1;
            info.blocks[i].isVisible = true;
            info.blocks[i].isDestructible = true;
            info.blocks[i].isInteractive = true;
            info.blocks[i].isCollisible = true;
        }

//        圆角修饰部分_out
        int index_out = StarUtils.realIndexOf(roundNumOutside, minTier);
        info.star_r = StarUtils.heightOf(tierNum*(tierNum-1)*3+1) * tierDistance;
        double dropHeight = baseTier * StarInfo.tierDistance;

        Range range = new Range(random.nextLong());
        long noiseSeed = random.nextLong();
        for (var i = roundNumOutside; i < groundNum; i++) {
            double percent = StarUtils.angleOf(index_out) / Math.PI / 2.0;
            double Random = range.Random(percent);
            if ( (StarUtils.heightOf(index_out) - info.star_r) * 2 / dropHeight
                    < (OpenSimplex2S.noise2(noiseSeed, percent * noiseLength, 0) + 1) * Random) {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
            }else { info.blocks[i].type = 0; info.blocks[i].variant = 0; }
            index_out++;
        }

//        表面
        for (var i: StarUtils.surfaceBlocks(0, minTier, tierInside, info)) {
            info.blocks[i].isDestructible = false;
            info.blocks[i].isInteractive = false;
            info.blocks[i].isSurface = true;
        }

        for (var i: StarUtils.surfaceBlocks(0, roundStartTier, tierNum + baseTier, info)) {
            info.blocks[i].type = 1;
            info.blocks[i].variant = 1;
            info.blocks[i].isSurface = true;
        }

/*
//        纯天空生成
        for (var i = groundNum; i < StarUtils.blockNum; i++) {
            info.blocks[i].type = 0;
        }
*/

//        石头
        int index = StarUtils.realIndexOf(0, minTier);
        noiseSeed = random.nextLong();
        range = new Range(random.nextLong());
        for (int i = 0; i < groundNum; i++) {
            if (info.blocks[i].isVisible) {
                Position pos = StarUtils.posOf(index);
                double height = Math.hypot(pos.x, pos.y);
                double percent = StarUtils.angleOf(index) / Math.PI / 2.0;
                double Random = range.Random(percent);
                if ((OpenSimplex2S.noise2(noiseSeed, pos.x / 10, pos.y / 10) + 1)
                        > 2 * (Math.atan((height / info.star_r - (0.8 + 0.2 * Random)) * 10) + Math.PI / 2.0) / Math.PI) {
                    info.blocks[i].type = 2;
                    info.blocks[i].variant = 0;
                }
            }
            index++;
        }

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

//        /**创建属于你的星球
//     * @param seed random
//     * */
//    public static StarInfo CreatMyStar(int seed) {
//        StarInfo MyStar = new StarInfo();
//        StarInfo.design = MyStar;
//        MyStar = gen(seed);
//        return MyStar;
//    }

    public static void main(String[] args) {
/*
        for (int i = 0; i < 100; i++) {
            System.out.println(OpenSimplex2S.noise2(0, i, 0) + 1);
        }
        System.out.println(OpenSimplex2S.noise2(0, 6, 0) + 1);
        System.out.println(Math.tan(0.8 * Math.PI / 2));
*/
    }
}