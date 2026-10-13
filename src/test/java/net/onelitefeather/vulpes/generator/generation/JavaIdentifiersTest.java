package net.onelitefeather.vulpes.generator.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaIdentifiersTest {

    @Test
    void constantNameReplacesDashAndDotWithUnderscore() {
        assertEquals("MY_ITEM_V2", JavaIdentifiers.constantName("my-item.v2"),
                "Every character outside [A-Z0-9_] must become an underscore");
    }

    @Test
    void constantNameCollapsesRepeatedUnderscores() {
        assertEquals("A_B_C", JavaIdentifiers.constantName("a--b..c"),
                "Repeated underscores produced by replacement must be collapsed into one");
    }

    @Test
    void constantNamePrefixesLeadingDigitWithUnderscore() {
        assertEquals("_2FAST", JavaIdentifiers.constantName("2fast"),
                "A leading digit is not a valid Java identifier start and needs an underscore before it");
    }

    @Test
    void constantNameKeepsAlreadyValidName() {
        assertEquals("MY_SWORD", JavaIdentifiers.constantName("MY_SWORD"),
                "A valid name must not be changed apart from the upper case");
    }

    @Test
    void localNameLowerCasesAndAppliesPrefix() {
        assertEquals("adv_my_item_v2", JavaIdentifiers.localName("adv_", "My-Item.V2"),
                "The local name must be the prefix followed by the lower case sanitized key");
    }

    @Test
    void localNameAppendsUnderscoreToJavaKeyword() {
        assertEquals("class_", JavaIdentifiers.localName("", "class"),
                "A local name that is a Java keyword must get an underscore suffix");
    }

    @Test
    void localNamePrefixesLeadingDigitWhenNoPrefixIsGiven() {
        assertEquals("_9lives", JavaIdentifiers.localName("", "9Lives"),
                "A local name without prefix that starts with a digit must get an underscore before it");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", ".", "__", "--.."})
    void rejectsKeysWithoutLetterOrDigit(String key) {
        assertThrows(IllegalArgumentException.class, () -> JavaIdentifiers.constantName(key),
                "A key without any letter or digit must not produce an identifier silently");
    }

    @Test
    void claimReturnsIdentifierWhenNoOtherKeyUsesIt() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();

        assertEquals("MY_ITEM", claims.claim("MY_ITEM", "my-item"),
                "The first key that claims an identifier must receive it");
    }

    @Test
    void claimAcceptsSameKeyTwice() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        claims.claim("MY_ITEM", "my-item");

        assertEquals("MY_ITEM", claims.claim("MY_ITEM", "my-item"),
                "Claiming the identifier again for the same key must not be a collision");
    }

    @Test
    void claimThrowsNamingBothKeysOnCollision() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        claims.claim("MY_ITEM", "my-item");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> claims.claim("MY_ITEM", "my.item"),
                "Two keys with the same identifier must abort the generation");

        assertTrue(exception.getMessage().contains("my-item"),
                "The message must name the first key but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("my.item"),
                "The message must name the second key but was: " + exception.getMessage());
    }
}
