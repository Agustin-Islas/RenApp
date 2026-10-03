import 'package:flutter/material.dart';

class AnimatedPushGesture extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;

  const AnimatedPushGesture({
    super.key,
    required this.child,
    this.onTap,
  });

  @override
  State<AnimatedPushGesture> createState() => _AnimatedPushGestureState();
}

class _AnimatedPushGestureState extends State<AnimatedPushGesture> {
  bool _isPressed = false;

  void _handleTapDown(TapDownDetails details) {
    if (widget.onTap != null) {
      setState(() => _isPressed = true);
    }
  }

  void _handleTapUp(TapUpDetails details) {
    if (widget.onTap != null) {
      setState(() => _isPressed = false);
      widget.onTap!();
    }
  }

  void _handleTapCancel() {
    if (widget.onTap != null) {
      setState(() => _isPressed = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: _handleTapDown,
      onTapUp: _handleTapUp,
      onTapCancel: _handleTapCancel,
      behavior: HitTestBehavior.opaque,
      child: AnimatedScale(
        scale: _isPressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 150),
        curve: Curves.easeOutCubic,
        child: widget.child,
      ),
    );
  }
}
