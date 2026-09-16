package com.trustagro.executive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExecutiveDateRangeTest {

    @Test
    void todayIsSingleDayInclusive() {
        ExecutiveDateRange r = ExecutiveDateRange.resolve("TODAY", null, null);
        assertEquals(LocalDate.now(), r.from());
        assertEquals(LocalDate.now(), r.to());
        assertEquals("TODAY", r.preset());
    }

    @Test
    void previousMonthExcludesCurrentMonth() {
        LocalDate today = LocalDate.now();
        ExecutiveDateRange r = ExecutiveDateRange.resolve("PREVIOUS_MONTH", null, null);
        assertTrue(r.to().isBefore(today.withDayOfMonth(1)));
        assertEquals(1, r.from().getDayOfMonth());
    }

    @Test
    void customRequiresBothDates() {
        assertThrows(IllegalArgumentException.class,
                () -> ExecutiveDateRange.resolve("CUSTOM", LocalDate.now(), null));
    }
}
