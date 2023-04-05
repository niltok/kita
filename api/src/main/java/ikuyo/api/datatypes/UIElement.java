package ikuyo.api.datatypes;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class UIElement {
    public String type, title;
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
        this.style.mergeIn(style, true);
        return this;
    }

    public UIElement withClass(String... classes) {
        this.classes = classes;
        return this;
    }

    public UIElement withTitle(String title) {
        this.title = title;
        return this;
    }

    public UIElement appendClass(String... classes) {
        var list = new ArrayList<>(List.of(this.classes));
        list.addAll(List.of(classes));
        this.classes = list.toArray(String[]::new);
        return this;
    }

    public static UIElement labelItem(UIElement left, UIElement right, JsonObject callback) {
        var clickable = callback != null && !callback.isEmpty();
        return new UIElement.Callback("div", callback,
                new UIElement("span", left),
                new UIElement("span", right)
        ).withClass("label-item", clickable ? "pointer-cursor" : "normal-cursor");
    }

    public static UIElement labelItem(UIElement left, UIElement right) {
        return new UIElement("div",
                new UIElement("span", left),
                new UIElement("span", right)
        ).withClass("label-item", "normal-cursor");
    }

    public static UIElement labelItem(UIElement left, UIElement right, double percent, String color) {
        var style = JsonObject.of("width", "%f%%".formatted(percent * 100));
        if (color != null && !color.isEmpty()) style.put("background-color", color);
        return new UIElement("div",
                new UIElement("div").withClass("label-percent").withStyle(style),
                new UIElement("span", left),
                new UIElement("span", right)
        ).withClass("label-item", "no-overflow", "relative", "normal-cursor");
    }

    public static UIElement labelItem(String left, String right, double percent, String color) {
        return labelItem(new UIElement.Text(left), new UIElement.Text(right), percent, color);
    }

    public static UIElement labelItem(String left, String right, double percent) {
        return labelItem(left, right, percent, null);
    }

    public static UIElement labelItem(String left, String right) {
        return labelItem(new UIElement.Text(left), new UIElement.Text(right));
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
