package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = Block.Normal.class, name = "normal")
//})
public abstract sealed class Block {
    public int type;
    ///可见性
    public boolean isVisible;
    ///可交互性
    public boolean isInteractive;
    ///可破坏性
    public boolean isDestructible;
    public static final class Normal extends Block {}
}
