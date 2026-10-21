import 'package:flutter/material.dart';
import 'package:frontend_dialysis_record/features/sessions/models/session_dto.dart';

enum WeeklyFluidStatus {
  ultrafiltration,
  retention,
  neutral,
  noData,
}

class WeeklyFluidSummaryItem {
  final int weekNumber;
  final String dateRangeLabel;
  final int averageValue; // Magnitud absoluta en ml/día
  final WeeklyFluidStatus status;
  final int closedDays;
  final int rawSum;

  const WeeklyFluidSummaryItem({
    required this.weekNumber,
    required this.dateRangeLabel,
    required this.averageValue,
    required this.status,
    required this.closedDays,
    required this.rawSum,
  });

  /// Valor formateado con signo semántico según el estado:
  /// - UF: "+380 ml/día"
  /// - Retención: "150 ml/día" (sin signo menos, absoluto)
  /// - Neutro: "0 ml/día"
  /// - Sin datos: "-- ml/día"
  String get formattedValue {
    switch (status) {
      case WeeklyFluidStatus.ultrafiltration:
        return '+$averageValue ml/día';
      case WeeklyFluidStatus.retention:
        return '$averageValue ml/día';
      case WeeklyFluidStatus.neutral:
        return '0 ml/día';
      case WeeklyFluidStatus.noData:
        return '-- ml/día';
    }
  }

  /// Etiqueta del chip de estado: "UF", "Retención", "Neutro", "Sin datos"
  String get statusLabel {
    switch (status) {
      case WeeklyFluidStatus.ultrafiltration:
        return 'UF';
      case WeeklyFluidStatus.retention:
        return 'Retención';
      case WeeklyFluidStatus.neutral:
        return 'Neutro';
      case WeeklyFluidStatus.noData:
        return 'Sin datos';
    }
  }

  /// Tooltip explicativo según el estado clínico (Opción 2 aprobada):
  String get tooltipMessage {
    switch (status) {
      case WeeklyFluidStatus.ultrafiltration:
        return 'Ultrafiltración (UF): Promedio diario de líquido extraído. Indica que eliminó más líquido del que ingresó.';
      case WeeklyFluidStatus.retention:
        return 'Retención: Promedio diario de líquido retenido. Indica que salió menos líquido del que ingresó.';
      case WeeklyFluidStatus.neutral:
        return 'Neutro: Promedio diario neutro. La infusión y el drenaje fueron iguales.';
      case WeeklyFluidStatus.noData:
        return 'Sin datos: Período sin jornadas clínicas finalizadas.';
    }
  }
}

class MonthlyUltrafiltrationSummary {
  final int totalChanges;
  final List<int> weeklyUltrafiltration;
  final List<int> weekDayCounts;
  final int elapsedDays;
  final List<WeeklyFluidSummaryItem> weeklyItems;

  const MonthlyUltrafiltrationSummary({
    required this.totalChanges,
    required this.weeklyUltrafiltration,
    required this.weekDayCounts,
    required this.elapsedDays,
    required this.weeklyItems,
  });

  factory MonthlyUltrafiltrationSummary.empty(DateTime month) {
    final monthStart = DateUtils.dateOnly(DateTime(month.year, month.month, 1));
    final monthEnd = DateUtils.dateOnly(
      DateTime(month.year, month.month + 1, 0),
    );
    final now = DateUtils.dateOnly(DateTime.now());

    int elapsed;
    if (now.isBefore(monthStart)) {
      elapsed = 0;
    } else if (now.isAfter(monthEnd)) {
      elapsed = monthEnd.day;
    } else {
      elapsed = now.day - 1;
    }

    final ranges = [
      'Días 1 - 7',
      'Días 8 - 14',
      'Días 15 - 21',
      'Días 22 - ${monthEnd.day}',
    ];

    final emptyItems = List.generate(
      4,
      (i) => WeeklyFluidSummaryItem(
        weekNumber: i + 1,
        dateRangeLabel: ranges[i],
        averageValue: 0,
        status: WeeklyFluidStatus.noData,
        closedDays: 0,
        rawSum: 0,
      ),
    );

    return MonthlyUltrafiltrationSummary(
      totalChanges: 0,
      weeklyUltrafiltration: const [0, 0, 0, 0],
      weekDayCounts: [7, 7, 7, monthEnd.day - 21],
      elapsedDays: elapsed,
      weeklyItems: emptyItems,
    );
  }
}

class MonthlyUltrafiltrationCalculator {
  static MonthlyUltrafiltrationSummary calculate({
    required DateTime month,
    required List<SessionDto> sessions,
  }) {
    final monthStart = DateUtils.dateOnly(DateTime(month.year, month.month, 1));
    final monthEnd = DateUtils.dateOnly(
      DateTime(month.year, month.month + 1, 0),
    );

    final nowDateTime = DateTime.now();
    final currentClinicalDate = nowDateTime.hour < 5
        ? DateUtils.dateOnly(nowDateTime.subtract(const Duration(days: 1)))
        : DateUtils.dateOnly(nowDateTime);

    final actualWeekDayCounts = List<int>.filled(4, 0);
    for (int i = 1; i <= monthEnd.day; i++) {
      final currentDay = monthStart.add(Duration(days: i - 1));
      if (!currentDay.isBefore(currentClinicalDate)) break;

      final weekIndex = i <= 7
          ? 0
          : i <= 14
          ? 1
          : i <= 21
          ? 2
          : 3;
      actualWeekDayCounts[weekIndex]++;
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

      if (day.isBefore(monthStart) || day.isAfter(monthEnd)) continue;

      if (!day.isBefore(currentClinicalDate)) continue;

      totalChanges++;
      final weekIndex = day.day <= 7
          ? 0
          : day.day <= 14
          ? 1
          : day.day <= 21
          ? 2
          : 3;
      if (weekIndex < 0 || weekIndex > 3) continue;
      weekTotals[weekIndex] +=
          session.partial ??
          ((session.infusion ?? 0) - (session.drainage ?? 0));
    }

    int elapsedDays;
    if (currentClinicalDate.isBefore(monthStart)) {
      elapsedDays = 0;
    } else if (currentClinicalDate.isAfter(monthEnd)) {
      elapsedDays = monthEnd.day;
    } else {
      elapsedDays = currentClinicalDate.day - 1;
    }

    final dateRanges = [
      'Días 1 - 7',
      'Días 8 - 14',
      'Días 15 - 21',
      'Días 22 - ${monthEnd.day}',
    ];

    final weeklyItems = <WeeklyFluidSummaryItem>[];
    for (int i = 0; i < 4; i++) {
      final closedDays = actualWeekDayCounts[i];
      final sum = weekTotals[i];

      if (closedDays == 0) {
        weeklyItems.add(
          WeeklyFluidSummaryItem(
            weekNumber: i + 1,
            dateRangeLabel: dateRanges[i],
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
          // Balance neto negativo = El paciente drenó más de lo que infundió (UF efectiva)
          status = WeeklyFluidStatus.ultrafiltration;
        } else if (sum > 0) {
          // Balance neto positivo = El paciente infundió más de lo que drenó (Retención hídrica)
          status = WeeklyFluidStatus.retention;
        } else {
          status = WeeklyFluidStatus.neutral;
        }

        weeklyItems.add(
          WeeklyFluidSummaryItem(
            weekNumber: i + 1,
            dateRangeLabel: dateRanges[i],
            averageValue: avg,
            status: status,
            closedDays: closedDays,
            rawSum: sum,
          ),
        );
      }
    }

    return MonthlyUltrafiltrationSummary(
      totalChanges: totalChanges,
      weeklyUltrafiltration: weeklyItems.map((e) {
        if (e.status == WeeklyFluidStatus.noData) return 0;
        return e.status == WeeklyFluidStatus.retention ? e.averageValue : e.averageValue;
      }).toList(),
      weekDayCounts: actualWeekDayCounts.map((c) => c == 0 ? 1 : c).toList(),
      elapsedDays: elapsedDays,
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
