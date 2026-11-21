package cn.org.autumn.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InputTest {

    @Test
    void of_nullAndBlank() {
        assertEquals("", Input.of(null, Input.BODY));
        assertEquals("", Input.of("   ", Input.NICK));
        assertEquals("", Input.of("\t\n", Input.NICK));
    }

    @Test
    void of_trimAndCollapse() {
        assertEquals("a b", Input.of("  a   b  ", Input.BODY));
        assertEquals("a b", Input.of("  a   b  ", 1));
        assertEquals("ab", Input.of("  a   b  ", Input.NONE));
        assertEquals("a", Input.of("　a", Input.NICK));
        assertEquals("a b", Input.of("a\u00A0\u00A0b", Input.REMARK));
    }

    @Test
    void of_spacesTruncatesTokens() {
        assertEquals("a b", Input.of("a  b   c  d", 1));
        assertEquals("a b c", Input.of("a b c d", Input.NICK));
        assertEquals("a b c d", Input.of("a b c d", Input.REMARK));
        assertEquals("张 三", Input.remark(" 张  三  "));
    }

    @Test
    void lines_preservesNewlines() {
        assertEquals("a b\nc d", Input.lines("  a   b  \n  c  d  ", Input.BODY));
        assertEquals("ab\ncd", Input.lines(" a  b \n c  d ", Input.NONE));
        assertEquals("line", Input.bodyLines("\n\n  line  \n\n"));
    }

    @Test
    void body_keepsEnglishSentenceSpaces() {
        String en = "This is a long English comment with many normal spaces between words";
        assertEquals(en, Input.body(en));
        assertEquals(en, Input.bodyLines(en));
        assertEquals("This is a", Input.nick(en));
    }

    @Test
    void sceneHelpers() {
        assertEquals("a b c", Input.nick("  a  b   c  d "));
        assertEquals("hello world", Input.remark("  hello   world  "));
        assertEquals("a@b.com", Input.none(" a @ b.com "));
        assertEquals("a b c d e", Input.body("  a  b   c  d  e  "));
    }
}
