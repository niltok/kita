package ikuyo.api.datatypes;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UIElement {
    public String type, title;
    public UIElement[] children;
    public JsonObject style = JsonObject.of();
    public List<String> classes = new ArrayList<>();
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

    public UIElement appendClass(String... classes) {
        this.classes.addAll(Arrays.asList(classes));
        return this;
    }

    public UIElement withTitle(String title) {
        this.title = title;
        return this;
    }

    public static UIElement labelItem(UIElement left, UIElement right, JsonObject callback) {
        var clickable = callback != null && !callback.isEmpty();
        return new UIElement.Callback("div", callback,
                new UIElement("span", left),
                new UIElement("span", right)
        ).appendClass("label-item", clickable ? "pointer-cursor" : "normal-cursor");
    }

    public static UIElement labelItem(UIElement left, UIElement right) {
        return new UIElement("div",
                new UIElement("span", left),
                new UIElement("span", right)
        ).appendClass("label-item", "normal-cursor");
    }

    public static UIElement labelItem(UIElement left, UIElement right, double percent, String color) {
        var style = JsonObject.of("width", "%f%%".formatted(percent * 100));
        if (color != null && !color.isEmpty()) style.put("background-color", color);
        return new UIElement("div",
                new UIElement("div").appendClass("label-percent").withStyle(style),
                new UIElement("span", left),
                new UIElement("span", right)
        ).appendClass("label-item", "no-overflow", "relative", "normal-cursor");
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

    public static UIElement normalLabel(String left, String right) {
        return labelItem(new UIElement.Text(left), new UIElement.Text(right)).appendClass("normal-label");
    }

    public static UIElement normalLabel(UIElement left, String right) {
        return labelItem(left, new UIElement.Text(right)).appendClass("normal-label");
    }

    public static UIElement hoverLabel(String left, String right) {
        return labelItem(new UIElement.Text(left), new UIElement.Text(right)).appendClass("hover-label");
    }

    public static UIElement text(String text) {
        return new UIElement.Text(text);
    }

    public static UIElement divText(String text) {
        return div(text(text));
    }

    public static UIElement callbackText(String text, JsonObject callback, String... states) {
        return new UIElement.Callback("div", callback, Arrays.stream(states).toList(), UIElement.text(text))
                .appendClass("label-item", "button", "hover-label");
    }

    public static UIElement div(UIElement... children) {
        return new UIElement("div", children);
    }

    public static UIElement span(UIElement... children) {
        return new UIElement("span", children);
    }

    public static class Callback extends UIElement {
        public JsonObject callback;
        public List<String> states = new ArrayList<>();
        public Callback(String type, JsonObject callback, String... states) {
            super(type);
            this.callback = callback;
            this.states = Arrays.stream(states).toList();
        }
        public Callback(String type, JsonObject callback, UIElement... children) {
            super(type, children);
            this.callback = callback;
        }

        public Callback(String type, JsonObject callback, List<String> states, UIElement... children) {
            super(type, children);
            this.callback = callback;
            this.states = states;
        }
    }

    public static class Stateful extends UIElement {
        public String stateName, value = "";
        public Stateful(String type, String stateName) {
            super(type);
            this.stateName = stateName;
        }
        public Stateful(String type, String stateName, String value) {
            super(type);
            this.stateName = stateName;
            this.value = value;
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
