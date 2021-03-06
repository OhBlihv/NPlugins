/***************************************************************************
 * Project file:    NPlugins - NCore - Click.java                          *
 * Full Class name: fr.ribesg.bukkit.api.chat.Click                        *
 *                                                                         *
 *                Copyright (c) 2012-2014 Ribesg - www.ribesg.fr           *
 *   This file is under GPLv3 -> http://www.gnu.org/licenses/gpl-3.0.txt   *
 *    Please contact me at ribesg[at]yahoo.fr if you improve this file!    *
 ***************************************************************************/

package fr.ribesg.bukkit.api.chat;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

/**
 * Represents an action that will be executed by the Minecraft Client as soon
 * as the player clicks on the {@link Part} this Click is attached to.
 *
 * @see Click.Type
 */
@SerializableAs("ChatClick")
public final class Click implements ConfigurationSerializable {

    /**
     * Regex matching allowed URLs as accepted by the Minecraft Client
     * as of Minecraft v1.7.10
     */
    private static final Pattern HTTP_REGEX = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);

    /**
     * Builds a new Click of type {@link Type#OPEN_URL}.
     *
     * @param url an URL matching {@link #HTTP_REGEX}
     *
     * @return a new Click of type OPEN_URL
     */
    public static Click ofOpenUrl(final String url) {
        Validate.notNull(url, "Url cannot be null!");
        Validate.isTrue(HTTP_REGEX.matcher(url).matches(), "Provided url is invalid: " + url);
        return forType(Type.OPEN_URL, url);
    }

    /**
     * Builds a new Click of type {@link Type#SEND_TEXT}.
     *
     * @param text a text
     *
     * @return a new Click of type SEND_TEXT
     */
    public static Click ofSendText(final String text) {
        return forType(Type.SEND_TEXT, text);
    }

    /**
     * Builds a new Click of type {@link Type#SET_TEXT}.
     *
     * @param text a text
     *
     * @return a new Click of type SET_TEXT
     */
    public static Click ofSetText(final String text) {
        return forType(Type.SET_TEXT, text);
    }

    /**
     * Builds a new Click of the provided Type with the provided text.
     *
     * @param type   the Type
     * @param action the text
     *
     * @return a new Click of the provided Type
     */
    private static Click forType(final Type type, final String action) {
        Validate.notEmpty(action);
        return new Click(type, action);
    }

    /**
     * An enum listing all possible action on Click.
     */
    public enum Type {
        /**
         * Based on Client configuration, will either open the URL
         * or open the "what to do with that URL?" prompt.
         */
        OPEN_URL,

        /**
         * Will make the user send the provided text to the chat.
         * Supports commands (text starting with '/').
         */
        SEND_TEXT,

        /**
         * Will fill the user chat input with the provided text.
         */
        SET_TEXT,;
    }

    /**
     * The Type of this Click
     */
    private final Type type;

    /**
     * The text of this Click
     */
    private final String text;

    /**
     * Builds a new Click of the provided Type with the provided text.
     *
     * @param type the Type
     * @param text the text
     */
    private Click(final Type type, final String text) {
        this.type = type;
        this.text = text;
    }

    /**
     * Gets the Type of this Click.
     *
     * @return the Type of this Click
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Gets the text of this Click.
     *
     * @return the text of this Click
     */
    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return "Click [type=" + this.type.name() + ", text=" + this.text + "]";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        final Click click = (Click)o;

        if (!this.text.equals(click.text)) {
            return false;
        }
        if (this.type != click.type) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.text.hashCode();
        return result;
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>of(
                "type", this.type.name(),
                "text", this.text
        );
    }

    /**
     * Converts the given {@link Map} to a Click chat message part.
     *
     * @param map the map to convert to a Click chat message part
     *
     * @see ConfigurationSerializable
     */
    public static Click deserialize(final Map<String, Object> map) {
        final Object typeName = map.get("type");
        if (typeName == null) {
            throw new IllegalArgumentException("Null is not a valid Click.Type");
        }
        final Type type;
        try {
            type = Type.valueOf((String)typeName);
        } catch (final Exception e) {
            throw new IllegalArgumentException(typeName + " is not a valid Click.Type", e);
        }
        final String text = (String)map.get("text");
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        return forType(type, text);
    }
}
