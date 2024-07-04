import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show ByteData, Uint8List, rootBundle;
import 'package:mobile/Pages/home/lesson_page.dart';

void main() {
  return runApp(const LearningPage());
}

class LearningPage extends StatefulWidget {
  const LearningPage({super.key});

  @override
  State<LearningPage> createState() => _LearningPageState();
}

class _LearningPageState extends State<LearningPage> {
  ValueNotifier<Offset> onTappedLocation = ValueNotifier(Offset.zero);
  ui.Image? backgroundImage;

  @override
  void initState() {
    super.initState();
    _loadImage();
  }

  Future<void> _loadImage() async {
    final ByteData data = await rootBundle.load('assets/background.png');
    final ui.Image image = await _loadImageFromBytes(data.buffer.asUint8List());
    setState(() {
      backgroundImage = image;
    });
  }

  Future<ui.Image> _loadImageFromBytes(Uint8List imgBytes) async {
    final Completer<ui.Image> completer = Completer();
    ui.decodeImageFromList(imgBytes, (ui.Image img) {
      return completer.complete(img);
    });
    return completer.future;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      routes: {
        '/lesson1': (context) => const LessonPage(),
      },
      home: Scaffold(
        appBar: AppBar(
          toolbarHeight: 50,
          backgroundColor: Colors.amberAccent.shade100,
          title: const Text(
            'Learning Page',
            style: TextStyle(
              fontSize: 35,
              fontWeight: FontWeight.w800,
              fontFamily: 'dmSans',
            ),
          ),
        ),
        body: SingleChildScrollView(
          child: Builder(
            builder: (BuildContext context) {
              return GestureDetector(
                onTapDown: (details) {
                  onTappedLocation.value = details.localPosition;
                },
                child: CustomPaint(
                  painter: CoursePath(
                    onTappedLocation: onTappedLocation,
                    onTap: (int index) {
                      if (index == 0) {
                        WidgetsBinding.instance.addPostFrameCallback((_) {
                          Navigator.pushNamed(context, '/lesson1');
                        });
                      }
                    },
                    backgroundImage: backgroundImage,
                  ),
                  size: const Size(double.infinity, 1000),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class CoursePath extends CustomPainter {
  CoursePath({
    required this.onTappedLocation,
    required this.onTap,
    this.backgroundImage,
  }) : super(repaint: onTappedLocation);

  final ValueNotifier<Offset> onTappedLocation;
  final Function(int) onTap;
  final double circleRadius = 40;
  final ui.Image? backgroundImage;

  @override
  void paint(Canvas canvas, Size size) {
    if (backgroundImage != null) {
      paintImage(
        canvas: canvas,
        rect: Rect.fromLTWH(0, 0, size.width, size.height),
        image: backgroundImage!,
        fit: BoxFit.cover,
      );
    }

    Paint paint1 = Paint()
      ..color = const Color.fromRGBO(74, 91, 99, 1)
      ..strokeWidth = 12
      ..style = PaintingStyle.stroke;

    Paint paint2 = Paint()
      ..color = const Color.fromRGBO(198, 219, 210, 1);

    Paint paint3 = Paint()
      ..color = const Color.fromRGBO(74, 91, 99, 1)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 12;

    var halfWidth = size.width / 2;

    final List<Offset> points1 = <Offset>[
      Offset(halfWidth, size.height),
      Offset(40 + 3 * size.width / 4, (size.height + (4 * size.height / 5)) / 2),
      Offset(halfWidth, 4 * size.height / 5),
      Offset(160 - size.width / 4, ((4 * size.height / 5) + (3 * size.height / 5)) / 2),
      Offset(halfWidth, 3 * size.height / 5),
      Offset(40 + 3 * size.width / 4, ((3 * size.height / 5) + (2 * size.height / 5)) / 2),
      Offset(halfWidth, 2 * size.height / 5),
      Offset(160 - size.width / 4, ((2 * size.height / 5) + (size.height / 5)) / 2),
      Offset(halfWidth, size.height / 5),
      Offset(40 + 3 * size.width / 4, size.height / 10),
      Offset(halfWidth, 0),
    ];

    final List<Offset> points2 = <Offset>[
      Offset(halfWidth, size.height),
      Offset(3 * size.width / 4, (size.height + (4 * size.height / 5)) / 2),
      Offset(halfWidth, 4 * size.height / 5),
      Offset(size.width / 4, ((4 * size.height / 5) + (3 * size.height / 5)) / 2),
      Offset(halfWidth, 3 * size.height / 5),
      Offset(3 * size.width / 4, ((3 * size.height / 5) + (2 * size.height / 5)) / 2),
      Offset(halfWidth, 2 * size.height / 5),
      Offset(size.width / 4, ((2 * size.height / 5) + (size.height / 5)) / 2),
      Offset(halfWidth, size.height / 5),
      Offset(3 * size.width / 4, size.height / 10),
      Offset(halfWidth, 0),
    ];

    var path = Path();

    path.moveTo(points1[0].dx, points1[0].dy);

    for (var i = 0; i < points1.length - 1; i++) {
      var xc = (points1[i].dx + points1[i + 1].dx) / 2;
      var yc = (points1[i].dy + points1[i + 1].dy) / 2;

      path.quadraticBezierTo(points1[i].dx, points1[i].dy, xc, yc);
    }

    path.quadraticBezierTo(
      points1[points1.length - 2].dx,
      points1[points1.length - 2].dy,
      points1[points1.length - 1].dx,
      points1[points1.length - 1].dy,
    );

    canvas.drawPath(path, paint1);

    ui.ParagraphStyle paragraphStyle = ui.ParagraphStyle(fontSize: 30);
    ui.TextStyle textStyle = ui.TextStyle(color: Colors.black87);

    for (var i = 0; i < points2.length; i++) {
      canvas.drawCircle(points2[i], circleRadius, paint2);
      canvas.drawCircle(points2[i], circleRadius, paint3);

      ui.ParagraphBuilder paragraphBuilder = ui.ParagraphBuilder(paragraphStyle);
      paragraphBuilder.pushStyle(textStyle);
      paragraphBuilder.addText("${i + 1}");
      ui.Paragraph paragraph = paragraphBuilder.build();
      paragraph.layout(const ui.ParagraphConstraints(width: double.infinity));

      Offset textOffset = Offset(
        points2[i].dx - paragraph.maxIntrinsicWidth / 2,
        points2[i].dy - paragraph.height / 2,
      );

      canvas.drawParagraph(paragraph, textOffset);
    }

    for (var point in points2) {
      if ((onTappedLocation.value - point).distance <= circleRadius) {
        onTap(points2.indexOf(point));
        break;
      }
    }
  }

  @override
  bool shouldRepaint(CoursePath oldDelegate) => false;
}