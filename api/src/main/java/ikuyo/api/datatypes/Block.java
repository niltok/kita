package ikuyo.api.datatypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = Block.Normal.class, name = "normal")
//})
public abstract sealed class Block {
    /**块类型<p/>
     * 0: 空气<br/>
     * 1: 土块<br/>
     * 2: 石块<br/>*/
    public int type = 0;
    /**变体贴图*/
    public int variant = 0;
    /**可见性*/
    public boolean isVisible = false;
    /**可交互性*/
    public boolean isInteractive = false;
    /**可破坏性*/
    public boolean isDestructible = false;
    /**可碰撞性*/
    public boolean isCollisible = false;
    /**是否是表面块*/
    public boolean isSurface = false;
    public static final class Normal extends Block {}
}
