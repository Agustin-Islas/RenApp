import 'package:flutter/material.dart';
import 'package:frontend_dialysis_record/features/sessions/models/monthly_ultrafiltration_summary.dart';
import 'package:frontend_dialysis_record/features/sessions/models/session_dto.dart';

export 'package:frontend_dialysis_record/features/sessions/models/monthly_ultrafiltration_summary.dart'
    show WeeklyFluidStatus, WeeklyFluidSummaryItem;

class FourWeeksUltrafiltrationSummary {
  final int totalChanges;
  final List<int> weeklyUltrafiltration;
  final List<int> weekDayCounts;
  final int elapsedDays;
  final List<WeeklyFluidSummaryItem> weeklyItems;

  const FourWeeksUltrafiltrationSummary({
    required this.totalChanges,
    required this.weeklyUltrafiltration,
    required this.weekDayCounts,
    required this.elapsedDays,
    required this.weeklyItems,
  });

  factory FourWeeksUltrafiltrationSummary.empty() {
    final emptyItems = List.generate(
      4,
      (i) => WeeklyFluidSummaryItem(
        weekNumber: i + 1,
        dateRangeLabel: 'Días ${i * 7 + 1} - ${(i + 1) * 7}',
        averageValue: 0,
        status: WeeklyFluidStatus.noData,
        closedDays: 0,
        rawSum: 0,
      ),
    );

    return FourWeeksUltrafiltrationSummary(
      totalChanges: 0,
      weeklyUltrafiltration: const [0, 0, 0, 0],
      weekDayCounts: const [7, 7, 7, 7],
      elapsedDays: 28,
      weeklyItems: emptyItems,
    );
  }
}

class FourWeeksUltrafiltrationCalculator {
  static FourWeeksUltrafiltrationSummary calculate({
    required DateTime endDate,
    required List<SessionDto> sessions,
  }) {
    // endDate is today. startDate is today - 27 days. Total exactly 28 days.
    final end = DateUtils.dateOnly(endDate);
    final start = end.subtract(const Duration(days: 27));
    final weekDayCounts = <int>[7, 7, 7, 7];

    final nowDateTime = DateTime.now();
    final currentClinicalDate = nowDateTime.hour < 5
        ? DateUtils.dateOnly(nowDateTime.subtract(const Duration(days: 1)))
        : DateUtils.dateOnly(nowDateTime);

    // Ajustar conteo de días cerrados transcurridos
    for (int i = 0; i <= 27; i++) {
      final currentDay = start.add(Duration(days: i));
      if (!currentDay.isBefore(currentClinicalDate)) {
        final weekIndex = i ~/ 7;
        if (weekIndex >= 0 && weekIndex < 4) {
          weekDayCounts[weekIndex]--;
        }
      }
    }

    final weekTotals = List<int>.filled(4, 0);
    var totalChanges = 0;

    for (final session in sessions) {
      final dateValue = session.date;
      if (dateValue == null) continue;
      final date = _parseDate(dateValue);
      if (date == null) continue;

      var day = DateUtils.dateOnly(date);

      // Ajuste de jornada clínica: recambios post-medianoche pertenecen a la jornada anterior
      final hourStr = session.hour;
      if (hourStr != null && hourStr.isNotEmpty) {
        final timeParts = hourStr.split(':');
        if (timeParts.isNotEmpty) {
          final h = int.tryParse(timeParts[0]);
          if (h != null && h < 5) {
            day = day.subtract(const Duration(days: 1));
          }
        }
      }

      if (day.isBefore(start) || day.isAfter(end)) continue;

      if (!day.isBefore(currentClinicalDate)) continue;

      totalChanges++;

      final differenceInDays = day.difference(start).inDays;
      final weekIndex = differenceInDays ~/ 7;

      if (weekIndex < 0 || weekIndex > 3) continue;

      weekTotals[weekIndex] +=
          session.partial ??
          ((session.infusion ?? 0) - (session.drainage ?? 0));
    }

    String formatDayMonth(DateTime d) =>
        '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}';

    final weeklyItems = <WeeklyFluidSummaryItem>[];
    for (int i = 0; i < 4; i++) {
      final closedDays = weekDayCounts[i];
      final sum = weekTotals[i];
      final wStart = start.add(Duration(days: i * 7));
      final wEnd = start.add(Duration(days: i * 7 + 6));
      final weekRangeLabel = '${formatDayMonth(wStart)} - ${formatDayMonth(wEnd)}';

      if (closedDays <= 0) {
        weeklyItems.add(
          WeeklyFluidSummaryItem(
            weekNumber: i + 1,
            dateRangeLabel: weekRangeLabel,
            averageValue: 0,
            status: WeeklyFluidStatus.noData,
            closedDays: 0,
            rawSum: 0,
          ),
        );
      } else {
        final avg = (sum.abs() / closedDays).round();
        WeeklyFluidStatus status;
        if (sum < 0) {
          status = WeeklyFluidStatus.ultrafiltration;
        } else if (sum > 0) {
          status = WeeklyFluidStatus.retention;
        } else {
          status = WeeklyFluidStatus.neutral;
        }

        weeklyItems.add(
          WeeklyFluidSummaryItem(
            weekNumber: i + 1,
            dateRangeLabel: weekRangeLabel,
            averageValue: avg,
            status: status,
            closedDays: closedDays,
            rawSum: sum,
          ),
        );
      }
    }

    return FourWeeksUltrafiltrationSummary(
      totalChanges: totalChanges,
      weeklyUltrafiltration: weeklyItems.map((e) {
        if (e.status == WeeklyFluidStatus.noData) return 0;
        return e.status == WeeklyFluidStatus.retention ? e.averageValue : e.averageValue;
      }).toList(),
      weekDayCounts: weekDayCounts.map((c) => c <= 0 ? 1 : c).toList(),
      elapsedDays: weekDayCounts.fold(0, (s, c) => s + (c > 0 ? c : 0)),
      weeklyItems: weeklyItems,
    );
  }

  static DateTime? _parseDate(String? dateStr) {
    if (dateStr == null || dateStr.isEmpty) return null;
    final iso = DateTime.tryParse(dateStr);
    if (iso != null) return iso;
    final parts = dateStr.split(RegExp(r'[/.-]'));
    if (parts.length == 3) {
      final d = int.tryParse(parts[0]);
      final m = int.tryParse(parts[1]);
      final y = int.tryParse(parts[2]);
      if (d != null && m != null && y != null) {
        if (y > 1000 && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
          return DateTime(y, m, d);
        }
        if (d > 1000 && m >= 1 && m <= 12 && y >= 1 && y <= 31) {
          return DateTime(d, m, y);
        }
      }
    }
    return null;
  }
}
