import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const LauncherApp());
}

class LauncherApp extends StatelessWidget {
  const LauncherApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: _LauncherHome(),
    );
  }
}

class _LauncherHome extends StatelessWidget {
  const _LauncherHome();

  static const MethodChannel _platform =
      MethodChannel('launcher/native');

  void _adminGesture() {
    _platform.invokeMethod('requestAdminAuth');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // Gesture rahasia (admin)
          Positioned(
            left: 0,
            top: 0,
            width: 80,
            height: 80,
            child: GestureDetector(
              onLongPress: _adminGesture,
              behavior: HitTestBehavior.translucent,
              child: const SizedBox.shrink(),
            ),
          ),

          // Optional branding / status
          const Center(
            child: Text(
              'Home Launcher',
              style: TextStyle(
                color: Colors.white54,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
    );
  }
}