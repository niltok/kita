package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = Block.Normal.class, name = "normal")
//})
public abstract sealed class Block {
    public int id;
    public static final class Normal extends Block {}
}
