package net.minecraft.network.chat;

import java.util.Objects;

/** The stand-in for the game's ClickEvent: an action and its value (observed, TextProbe). Its codec isn't here. */
public class ClickEvent {
    private final Action action;
    private final String value;

    public ClickEvent(Action action, String value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClickEvent e && action == e.action && Objects.equals(value, e.value);
    }

    @Override
    public String toString() {
        return "ClickEvent{action=" + action + ", value='" + value + "'}";
    }

    @Override
    public int hashCode() {
        return 31 * action.hashCode() + (value != null ? value.hashCode() : 0);
    }

    /** The stand-in for ClickEvent.Action: its constants and names. */
    public enum Action implements net.minecraft.util.StringRepresentable {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", true),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true);

        private final String name;
        private final boolean allowFromServer;

        Action(String name, boolean allowFromServer) {
            this.name = name;
            this.allowFromServer = allowFromServer;
        }

        public boolean isAllowedFromServer() {
            return allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
