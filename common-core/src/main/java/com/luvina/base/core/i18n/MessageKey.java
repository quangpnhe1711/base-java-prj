package com.luvina.base.core.i18n;

/**
 * A localisable label, typically the display name of a domain concept
 * ("Sample", "Sample name") that gets substituted into an {@link ErrorCode}
 * message as {@code {0}}.
 *
 * <p>{@code common-core} deliberately ships no domain labels. Each service
 * declares its own enum:
 *
 * <pre>
 * public enum SampleMessageKey implements MessageKey {
 *     SAMPLE, SAMPLE_NAME, SAMPLE_CODE;
 *
 *     &#64;Override
 *     public String key() {
 *         return name();
 *     }
 * }
 * </pre>
 *
 * and adds the matching entries to its own {@code messages.properties}.
 */
public interface MessageKey {

    /**
     * Returns the resource-bundle key for this label.
     *
     * @return message key
     */
    String key();
}
