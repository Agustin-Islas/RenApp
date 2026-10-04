import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/foundation.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:frontend_dialysis_record/core/providers/providers.dart';
import 'package:frontend_dialysis_record/features/auth/models/me_response.dart';

/// Manages authentication state across the app.
class AuthNotifier extends AsyncNotifier<MeResponse?> {
  static const _storage = FlutterSecureStorage();
  static const _cachedMeKey = 'cached_me_response';

  @override
  Future<MeResponse?> build() async {
    // Escuchar cambios de sesión en Supabase para auto-actualizar el estado
    Supabase.instance.client.auth.onAuthStateChange.listen((data) {
      final session = data.session;
      if (session == null) {
        _storage.delete(key: _cachedMeKey);
        state = const AsyncData(null);
      } else {
        refresh();
      }
    });

    final session = Supabase.instance.client.auth.currentSession;
    if (session != null) {
      // 1. UI Optimista: Leer caché para carga instantánea (0 latencia)
      final cachedStr = await _storage.read(key: _cachedMeKey);
      if (cachedStr != null) {
        try {
          final cachedMe = MeResponse.fromJson(jsonDecode(cachedStr));
          // Refrescar en background sin bloquear la UI
          _fetchAndSync().then((freshMe) {
            if (freshMe != null) {
              state = AsyncData(freshMe);
            }
          }).catchError((e) {
            if (kDebugMode) debugPrint('Error sync background: $e');
          });
          return cachedMe;
        } catch (e) {
          if (kDebugMode) debugPrint('Error leyendo caché: $e');
        }
      }
    }

    return _fetchAndSync();
  }

  Future<MeResponse?> _fetchAndSync() async {
    final session = Supabase.instance.client.auth.currentSession;
    if (session == null) return null;

    final controller = ref.read(authControllerProvider);
    try {
      final me = await controller.getMe();
      if (me != null) {
        await _storage.write(key: _cachedMeKey, value: jsonEncode(me.toJson()));
      }
      return me;
    } catch (e) {
      // Si getMe falla, el usuario existe en Supabase pero quizás aún no en Spring Boot
      final user = Supabase.instance.client.auth.currentUser;
      if (user != null &&
          user.userMetadata != null &&
          user.userMetadata!.containsKey('role')) {
        try {
          final role = user.userMetadata!['role'];
          if (role == 'PATIENT') {
            await controller.registerPatient(
              email: user.email ?? '',
              name: user.userMetadata!['name'] ?? '',
              surname: user.userMetadata!['surname'] ?? '',
              dni: user.userMetadata!['dni'] ?? 0,
              dateOfBirth: user.userMetadata!['dateOfBirth'] ?? '',
              address: user.userMetadata!['address'] ?? '',
              number: user.userMetadata!['number'] ?? 0,
            );
          } else if (role == 'DOCTOR') {
            await controller.registerDoctor(
              email: user.email ?? '',
              name: user.userMetadata!['name'] ?? '',
              surname: user.userMetadata!['surname'] ?? '',
            );
          }
          // Tras registrar, volvemos a intentar getMe
          final me = await controller.getMe();
          if (me != null) {
            await _storage.write(key: _cachedMeKey, value: jsonEncode(me.toJson()));
          }
          return me;
        } catch (syncError) {
          if (kDebugMode) debugPrint('Error syncing with backend: $syncError');
          rethrow;
        }
      }
      rethrow;
    }
  }

  /// Clear session and redirect to login.
  Future<void> logout({bool global = false}) async {
    final scope = global ? SignOutScope.global : SignOutScope.local;
    await Supabase.instance.client.auth.signOut(scope: scope);
    await _storage.delete(key: _cachedMeKey);
    state = const AsyncData(null);
  }

  /// Refresh user data without clearing the session.
  Future<void> refresh() async {
    try {
      final me = await _fetchAndSync();
      state = AsyncData(me);
    } catch (e, st) {
      state = AsyncError(e, st);
    }
  }
}

final authStateProvider = AsyncNotifierProvider<AuthNotifier, MeResponse?>(
  AuthNotifier.new,
);
