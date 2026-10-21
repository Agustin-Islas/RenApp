import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:frontend_dialysis_record/core/config/app_config.dart';
import 'package:frontend_dialysis_record/core/network/app_exception.dart';

class DioClient {
  final Dio dio;

  static const String baseUrl = AppConfig.apiBaseUrl;

  DioClient()
    : dio = Dio(
        BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(milliseconds: 30000),
          receiveTimeout: const Duration(milliseconds: 30000),
          headers: const {'Content-Type': 'application/json'},
        ),
      ) {
    if (kDebugMode) {
      dio.interceptors.add(
        LogInterceptor(
          requestHeader: true,
          requestBody: true,
          responseHeader: false,
          responseBody: true,
          error: true,
        ),
      );
    }

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Obtener el token de acceso directamente de la sesión actual de Supabase
          var session = Supabase.instance.client.auth.currentSession;

          // Timeout mínimo reactivo en caso de arranque en frío
          if (session == null) {
            for (var i = 0; i < 6 && session == null; i++) {
              await Future.delayed(const Duration(milliseconds: 50));
              session = Supabase.instance.client.auth.currentSession;
            }
          }

          // Si el token está expirado, refrescarlo antes de despachar la petición
          if (session != null && session.isExpired) {
            try {
              final res = await Supabase.instance.client.auth.refreshSession();
              session = res.session ?? Supabase.instance.client.auth.currentSession;
            } catch (_) {}
          }

          final access = session?.accessToken;

          if (access != null && access.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $access';
          }
          handler.next(options);
        },
        onError: (e, handler) async {
          // Si el backend retorna 401 Unauthorized y existe sesión, reintentar refrescando token una vez
          if (e.response?.statusCode == 401) {
            try {
              final res = await Supabase.instance.client.auth.refreshSession();
              final newAccess = res.session?.accessToken;
              if (newAccess != null && newAccess.isNotEmpty) {
                final requestOptions = e.requestOptions;
                requestOptions.headers['Authorization'] = 'Bearer $newAccess';
                final cloneReq = await dio.fetch(requestOptions);
                return handler.resolve(cloneReq);
              }
            } catch (_) {}
          }

          handler.reject(
            DioException(
              requestOptions: e.requestOptions,
              response: e.response,
              type: e.type,
              error: AppException.fromDio(e),
              stackTrace: e.stackTrace,
              message: e.message,
            ),
          );
        },
      ),
    );
  }
}
