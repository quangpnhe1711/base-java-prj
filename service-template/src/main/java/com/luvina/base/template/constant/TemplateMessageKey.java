package com.luvina.base.template.constant;

import com.luvina.base.core.i18n.MessageKey;

/**
 * Domain labels of this service, substituted into the shared {@code ERRxxx}
 * messages.
 *
 * <p>Each constant needs a matching entry in {@code messages.properties} and in
 * every translation of it.
 */
public enum TemplateMessageKey implements MessageKey {

    /** Label of the sample entity. */
    SAMPLE,

    /** Label of the sample code field. */
    SAMPLE_CODE,

    /** Label of the sample name field. */
    SAMPLE_NAME;

    @Override
    public String key() {
        return name();
    }
}
