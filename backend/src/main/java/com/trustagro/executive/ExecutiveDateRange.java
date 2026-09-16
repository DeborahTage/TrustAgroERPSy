package com.trustagro.executive;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Inclusive executive reporting window.
 * <p>
 * Convention: {@code from} and {@code to} are both inclusive calendar dates.
 * Presets that describe "current" periods end at {@link LocalDate#now()} (not future
 * month-end). Previous-period presets use full prior calendar month/quarter.
 * {@code CUSTOM} requires both {@code from} and {@code to}.
 */
public record ExecutiveDateRange(LocalDate from, LocalDate to, String preset) {

    public static ExecutiveDateRange resolve(String preset, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        String p = (preset == null || preset.isBlank()) ? "THIS_MONTH" : preset.trim().toUpperCase();
        return switch (p) {
            case "TODAY" -> new ExecutiveDateRange(today, today, p);
            case "THIS_WEEK" -> {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new ExecutiveDateRange(start, today, p);
            }
            case "THIS_MONTH" -> new ExecutiveDateRange(today.withDayOfMonth(1), today, p);
            case "THIS_QUARTER" -> {
                int qStartMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(today.getYear(), qStartMonth, 1);
                yield new ExecutiveDateRange(start, today, p);
            }
            case "THIS_YEAR" -> new ExecutiveDateRange(today.withDayOfYear(1), today, p);
            case "PREVIOUS_MONTH" -> {
                LocalDate start = today.minusMonths(1).withDayOfMonth(1);
                LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
                yield new ExecutiveDateRange(start, end, p);
            }
            case "PREVIOUS_QUARTER" -> {
                LocalDate thisQStart = LocalDate.of(today.getYear(),
                        ((today.getMonthValue() - 1) / 3) * 3 + 1, 1);
                LocalDate start = thisQStart.minusMonths(3);
                LocalDate end = thisQStart.minusDays(1);
                yield new ExecutiveDateRange(start, end, p);
            }
            case "CUSTOM" -> {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("CUSTOM preset requires from and to dates");
                }
                if (to.isBefore(from)) {
                    throw new IllegalArgumentException("to must be on or after from");
                }
                yield new ExecutiveDateRange(from, to, p);
            }
            default -> new ExecutiveDateRange(today.withDayOfMonth(1), today, "THIS_MONTH");
        };
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("preset", preset);
        m.put("from", from.toString());
        m.put("to", to.toString());
        m.put("inclusive", true);
        return m;
    }
}
