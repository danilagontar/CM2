package depviz.nuget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты выбора версии по диапазону NuGet.
 */
class VersionRangeTest {

    @Test
    void takesLowerBound() {
        assertEquals("4.3.0", VersionRange.lowest("4.3.0"));
        assertEquals("1.6.1", VersionRange.lowest("[1.6.1, )"));
        assertEquals("1.0", VersionRange.lowest("[1.0]"));
        assertEquals("2.0.0-beta", VersionRange.lowest("(2.0.0-beta, 3.0)"));
    }

    @Test
    void returnsEmptyWithoutLowerBound() {
        assertEquals("", VersionRange.lowest("(, 2.0]"));
        assertEquals("", VersionRange.lowest("["));
        assertEquals("", VersionRange.lowest("1.0.*"));
    }
}
