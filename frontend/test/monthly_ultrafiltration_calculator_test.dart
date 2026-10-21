import 'package:flutter_test/flutter_test.dart';
import 'package:frontend_dialysis_record/features/sessions/models/monthly_ultrafiltration_summary.dart';
import 'package:frontend_dialysis_record/features/sessions/models/four_weeks_ultrafiltration_summary.dart';
import 'package:frontend_dialysis_record/features/sessions/models/session_dto.dart';

void main() {
  group('MonthlyUltrafiltrationCalculator Tests', () {
    test('Calculates UF and Retention correctly using algebraic sum and absolute magnitude', () {
      final month = DateTime(2026, 10, 1);

      // Session data for week 1: 3 sessions resulting in net negative balance (UF)
      // Day 1: infusion 2000, drainage 2300 => partial = -300
      // Day 2: infusion 2000, drainage 2450 => partial = -450
      // Day 3: infusion 2000, drainage 1900 => partial = +100
      // Sum = -300 + -450 + 100 = -650 (extraction won)
      final sessions = [
        SessionDto(date: '2026-10-01', hour: '08:00', infusion: 2000, drainage: 2300, partial: -300),
        SessionDto(date: '2026-10-02', hour: '08:00', infusion: 2000, drainage: 2450, partial: -450),
        SessionDto(date: '2026-10-03', hour: '08:00', infusion: 2000, drainage: 1900, partial: 100),
      ];

      final summary = MonthlyUltrafiltrationCalculator.calculate(
        month: month,
        sessions: sessions,
      );

      expect(summary.weeklyItems.length, 4);

      // Week 1 range should always be Días 1 - 7
      expect(summary.weeklyItems[0].dateRangeLabel, 'Días 1 - 7');

      // Week 4 for October (31 days) should be Días 22 - 31
      expect(summary.weeklyItems[3].dateRangeLabel, 'Días 22 - 31');
    });

    test('February date range ends on 28 (or 29)', () {
      final febMonth = DateTime(2025, 2, 1);
      final summary = MonthlyUltrafiltrationCalculator.calculate(
        month: febMonth,
        sessions: [],
      );

      expect(summary.weeklyItems[3].dateRangeLabel, 'Días 22 - 28');
    });

    test('November date range ends on 30', () {
      final novMonth = DateTime(2026, 11, 1);
      final summary = MonthlyUltrafiltrationCalculator.calculate(
        month: novMonth,
        sessions: [],
      );

      expect(summary.weeklyItems[3].dateRangeLabel, 'Días 22 - 30');
    });

    test('WeeklyFluidSummaryItem formatting matches clinical requirements', () {
      const ufItem = WeeklyFluidSummaryItem(
        weekNumber: 1,
        dateRangeLabel: 'Días 1 - 7',
        averageValue: 380,
        status: WeeklyFluidStatus.ultrafiltration,
        closedDays: 7,
        rawSum: -2660,
      );

      expect(ufItem.statusLabel, 'UF');
      expect(ufItem.formattedValue, '+380 ml/día');
      expect(ufItem.tooltipMessage, contains('Promedio diario de líquido extraído'));

      const retItem = WeeklyFluidSummaryItem(
        weekNumber: 2,
        dateRangeLabel: 'Días 8 - 14',
        averageValue: 150,
        status: WeeklyFluidStatus.retention,
        closedDays: 7,
        rawSum: 1050,
      );

      expect(retItem.statusLabel, 'Retención');
      // Must be semantic absolute, no negative sign!
      expect(retItem.formattedValue, '150 ml/día');
      expect(retItem.tooltipMessage, contains('Promedio diario de líquido retenido'));

      const noDataItem = WeeklyFluidSummaryItem(
        weekNumber: 3,
        dateRangeLabel: 'Días 15 - 21',
        averageValue: 0,
        status: WeeklyFluidStatus.noData,
        closedDays: 0,
        rawSum: 0,
      );

      expect(noDataItem.statusLabel, 'Sin datos');
      expect(noDataItem.formattedValue, '-- ml/día');
      expect(noDataItem.tooltipMessage, contains('Período sin jornadas clínicas finalizadas'));
    });
  });

  group('FourWeeksUltrafiltrationCalculator Tests', () {
    test('Formats 4-week ranges as explicit calendar dates dd/MM - dd/MM', () {
      final endDate = DateTime(2026, 10, 21);
      final summary = FourWeeksUltrafiltrationCalculator.calculate(
        endDate: endDate,
        sessions: [],
      );

      expect(summary.weeklyItems.length, 4);
      // Start is 2026-10-21 - 27 days = 2026-09-24
      expect(summary.weeklyItems[0].dateRangeLabel, '24/09 - 30/09');
      expect(summary.weeklyItems[1].dateRangeLabel, '01/10 - 07/10');
      expect(summary.weeklyItems[2].dateRangeLabel, '08/10 - 14/10');
      expect(summary.weeklyItems[3].dateRangeLabel, '15/10 - 21/10');
    });
  });
}
