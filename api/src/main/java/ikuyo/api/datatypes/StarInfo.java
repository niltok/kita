package ikuyo.api.datatypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.utils.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class StarInfo {
    public static final double sqrt3 = 1.732050807568877;
    /**层级最大值*/
    @SuppressWarnings("PointlessArithmeticExpression")
    public static final int maxTier = StarUtils.areaSize * (StarUtils.areaTier / 2 * 3 + 1 + StarUtils.areaTier % 2);
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

    public static ByteArrayOutputStream genStarInfo(String users, String[] blocks) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            var mapper = DataStatic.mapper;
            try (var gen = mapper.createGenerator(output)) {
                gen.writeStartObject();
                gen.writeArrayFieldStart("blocks");
                for (String block : blocks) {
                    gen.writeRawValue(block);
                }
                gen.writeEndArray();
                gen.writeFieldName("starUsers");
                gen.writeRawValue(users);
                gen.writeEndObject();
            }
            return output;
        }
    }

    @Override
    public String toString() {
        try {
            return DataStatic.mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Buffer toBuffer() {
        try {
            return DataStatic.gzipEncode(DataStatic.mapper.writeValueAsBytes(this));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public static StarInfo generate(int seed) {
        var info = new StarInfo();
        Random random = new Random(seed);
        info.blocks = new Block[StarUtils.blockNum];
        IntStream.range(0, info.blocks.length).parallel().forEach(i -> {
            var block = new Block.Normal();
            info.blocks[i] = block;
            block.variant = 0;
        });
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
        int indexInside = StarUtils.realIndexOf(0);
        double rInside = StarUtils.heightOf(indexInside);
        int tierInside = (int)((Math.sqrt(3)*2/3-1) * rInside) + 1 + minTier;
        int roundNumInside = tierInside*(tierInside+1)*3 - minTier *(minTier -1)*3;
        if (roundNumInside > StarUtils.blockNum) roundNumInside = StarUtils.blockNum;
        IntStream.range(0, roundNumInside).parallel().forEach(i -> {
            if (StarUtils.heightOf(StarUtils.realIndexOf(i)) < rInside) info.blocks[i].type = 0;
            else {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
                info.blocks[i].drop = CargoStatic.soil.type();
            }
        });

//        计算地表
        int roundStartTier = (int)(tierNum * tierDistance) - 1;
        int groundNum =  (tierNum + baseTier) * ((tierNum+baseTier)+1) * 3 - minTier * (minTier -1) * 3;
        int roundNumOutside = roundStartTier*(roundStartTier+1)*3 - minTier *(minTier -1)*3;
        if (roundNumOutside < 0) roundNumOutside = 0;

//        纯地面生成
        IntStream.range(roundNumInside, roundNumOutside).parallel().forEach(i -> {
            var block = info.blocks[i];
            block.type = 1;
            block.isVisible = true;
            block.isDestructible = true;
            block.isInteractive = true;
            block.isCollisible = true;
            info.blocks[i].drop = CargoStatic.soil.type();
        });

//        圆角修饰部分_out
//        int index_out = StarUtils.realIndexOf(roundNumOutside);
        info.star_r = StarUtils.heightOf(tierNum*(tierNum-1)*3+1) * tierDistance;
        double dropHeight = baseTier * StarInfo.tierDistance;

        Range range = new Range(random.nextLong()), tempRange = range;
        long noiseSeed = random.nextLong(), tempSeed = noiseSeed;
        IntStream.range(roundNumOutside, groundNum).parallel().forEach(i -> {
            int index_out = StarUtils.realIndexOf(i);
            double percent = StarUtils.angleOf(index_out) / Math.PI / 2.0;
            double Random = tempRange.Random(percent);
            if ( (StarUtils.heightOf(index_out) - info.star_r) * 2 / dropHeight
                    < (OpenSimplex2S.noise2(tempSeed, percent * noiseLength, 0) + 1) * Random) {
                info.blocks[i].type = 1;
                info.blocks[i].isVisible = true;
                info.blocks[i].isDestructible = true;
                info.blocks[i].isInteractive = true;
                info.blocks[i].isCollisible = true;
                info.blocks[i].drop = CargoStatic.soil.type();
            }else { info.blocks[i].type = 0; info.blocks[i].variant = 0; }
        });

//        表面
        StarUtils.surfaceBlocks(0, minTier, tierInside, info).stream().parallel().forEach(i -> {
            var block = info.blocks[i];
            block.isDestructible = false;
            block.isInteractive = false;
            block.isSurface = true;
        });

        StarUtils.surfaceBlocks(
                0, roundStartTier, tierNum + baseTier, info
        ).stream().parallel().forEach(i -> {
            var block = info.blocks[i];
            block.type = 1;
            block.variant = 1;
            block.isSurface = true;
        });

/*
//        纯天空生成
        for (var i = groundNum; i < StarUtils.blockNum; i++) {
            info.blocks[i].type = 0;
        }
*/

//        石头
        creatBlocks(info, 2, 0, CargoStatic.stone.type(), groundNum, 10, 0.8, 0, random);
//        铁矿
        creatBlocks(info, 40, 4, CargoStatic.iron.type(), groundNum, 30, -1, 0.93, random);
//        铜矿
        creatBlocks(info, 3, 0, CargoStatic.copper.type(), groundNum, 30, -1, 0.93, random);
//        水晶矿
        creatBlocks(info, 5, 0, CargoStatic.crystal.type(), groundNum, 50, -1, 0.98, random);
//        金矿
        creatBlocks(info, 4, 0, CargoStatic.gold.type(), groundNum, 50, -1, 0.97, random);

        return info;
    }

    /**生成指定类型块
     * @param distribution 块分布密度，影响图稠密度。值越大越稀疏，但是相对的，对应 unchangedDensity 值的面积会变大。
     * @param changeHeight 层级分布的分界线，值为 -1 时为均匀分布
     * @param unchangedDensity 均匀分布时的指定密度
     * */
    private static void creatBlocks(StarInfo info, int type, int variant, String drop,
                                    int blockNum, double distribution, double changeHeight, double unchangedDensity, Random random) {
        int index = StarUtils.realIndexOf(0);
        long noiseSeed = random.nextLong();
        Range range = new Range(random.nextLong());
        for (int i = 0; i < blockNum; i++) {
            if (info.blocks[i].isVisible) {
                Position pos = StarUtils.positionOf(index);
                double height = 0;
                if (changeHeight != -1)
                    height = Math.hypot(pos.x, pos.y);
                double percent = StarUtils.angleOf(index) / Math.PI / 2.0;
                double newRandom = range.Random(percent);
                if ((OpenSimplex2S.noise2(noiseSeed, pos.x / distribution, pos.y / distribution) + 1)
                        > 2 * (changeHeight == -1 ? unchangedDensity :
                        (Math.atan((height / info.star_r - (changeHeight + (1 - changeHeight) * newRandom)) * 10) + Math.PI / 2.0) / Math.PI)) {
                    info.blocks[i].type = type;
                    info.blocks[i].variant = variant;
                    info.blocks[i].drop = drop;
                }
            }
            index++;
        }
    }

    public static StarInfo fromJson(String str) {
        return fromJson(new JsonObject(str));
    }

    public static StarInfo fromJson(Buffer buffer) {
        try {
            return DataStatic.mapper.readValue(DataStatic.gzipDecode(buffer), StarInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static StarInfo fromJson(JsonObject json) {
        return json.mapTo(StarInfo.class);
    }

//        /**creat your own star!
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