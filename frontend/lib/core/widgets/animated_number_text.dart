import 'package:flutter/material.dart';

class AnimatedNumberText extends StatelessWidget {
  final double number;
  final TextStyle? style;
  final String Function(double)? formatter;
  final Duration duration;

  const AnimatedNumberText({
    super.key,
    required this.number,
    this.style,
    this.formatter,
    this.duration = const Duration(milliseconds: 500),
  });

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(begin: 0, end: number),
      duration: duration,
      curve: Curves.easeOutCubic,
      builder: (context, value, child) {
        final displayString = formatter != null 
            ? formatter!(value) 
            : value.toStringAsFixed(0);
        return Text(
          displayString,
          style: style,
        );
      },
    );
  }
}
