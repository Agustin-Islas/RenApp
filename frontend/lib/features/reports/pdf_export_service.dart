import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:universal_html/html.dart' as html;
import 'package:path_provider/path_provider.dart';
import 'package:open_filex/open_filex.dart';
import 'package:frontend_dialysis_record/core/widgets/app_snackbar.dart';

class PdfExportService {
  static Future<void> viewOrDownloadPdf({
    required BuildContext context,
    required Uint8List pdfBytes,
    required String filename,
  }) async {
    if (kIsWeb) {
      // Abre en nueva pestaña nativa usando Blob
      final blob = html.Blob([pdfBytes], 'application/pdf');
      final url = html.Url.createObjectUrlFromBlob(blob);
      html.window.open(url, '_blank');
    } else {
      try {
        // En móvil, guarda temporalmente y delega al OS (Quick Look / Visor Nativo)
        final directory = await getTemporaryDirectory();
        final path = '${directory.path}/$filename';
        final file = File(path);
        await file.writeAsBytes(pdfBytes);
        
        final result = await OpenFilex.open(path);
        
        if (result.type != ResultType.done && context.mounted) {
          AppSnackBar.showException(
            context,
            Exception(result.message),
            'No hay visor de PDF instalado',
          );
        }
      } catch (e) {
        if (context.mounted) {
          AppSnackBar.showException(context, e, 'Error al abrir el PDF');
        }
      }
    }
  }
}
