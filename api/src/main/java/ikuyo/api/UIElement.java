package ikuyo.api;

import io.vertx.core.json.JsonObject;

public class UIElement {
    public String type;
    public UIElement[] children;
    public JsonObject style = JsonObject.of();
    public String[] classes = new String[0];
    public UIElement(String type) {
        this.type = type;
        this.children = new UIElement[0];
    }
    public UIElement(String type, UIElement... children) {
        this.type = type;
        this.children = children;
    }
    public UIElement withStyle(JsonObject style) {
        this.style = style;
        return this;
    }

    public UIElement withClass(String... classes) {
        this.classes = classes;
        return this;
    }

    public static class Callback extends UIElement {
        public JsonObject callback;
        public Callback(String type, JsonObject callback) {
            super(type);
            this.callback = callback;
        }
        public Callback(String type, JsonObject callback, UIElement... children) {
            super(type, children);
            this.callback = callback;
        }
    }

    public static class Stateful extends UIElement {
        public String stateName;
        public Stateful(String type, String stateName) {
            super(type);
            this.stateName = stateName;
        }
        public Stateful(String type, String stateName, UIElement... children) {
            super(type, children);
            this.stateName = stateName;
        }
    }

    public static class Text extends UIElement {
        public String text;
        public Text(String text) {
            super("text");
            this.text = text;
        }
    }
}
