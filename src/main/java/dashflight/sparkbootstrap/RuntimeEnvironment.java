package dashflight.sparkbootstrap;

import java.util.Arrays;

/**
 * Representation of the java runtime environment. Used to
 * configure request headers and cookie attributes.
 */
public enum RuntimeEnvironment {
    // Used for local development
    DEVELOPMENT("development"),

    // Used for staging environment
    STAGING("staging"),

    // Used in production environment
    PRODUCTION("production");


    private String text;

    RuntimeEnvironment(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    /**
     * Converts a String to a RuntimeEnvironment enum. Is case-insensitive.
     */
    public static RuntimeEnvironment fromString(String text) {
        for (RuntimeEnvironment enumValue : RuntimeEnvironment.values()) {
            if (enumValue.text.equalsIgnoreCase(text)) {
                return enumValue;
            }
        }

        String options = Arrays.stream(RuntimeEnvironment.values())
                .map(RuntimeEnvironment::getText)
                .reduce((s, s2) -> String.format("%s, %s", s, s2))
                .orElse("");

        throw new IllegalArgumentException(String.format("`%s` is not a valid environment. Available environments: [%s]", text, options));
    }

}
