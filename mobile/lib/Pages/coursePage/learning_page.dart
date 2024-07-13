import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/Pages/coursePage/lesson_page.dart';

class LearningPage extends StatefulWidget {
  final String moduleName;
  final String subfolder;
  const LearningPage(
      {required this.moduleName, required this.subfolder, super.key});

  @override
  State<LearningPage> createState() => _LearningPageState();
}

class _LearningPageState extends State<LearningPage> {
  void _navigateToLesson(int index) {
    Future.microtask(() {
      Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => LessonPage(
                    lessonName: "Lesson ${index + 1}",
                    fileName: "${widget.subfolder.split('_')[0]}_${index + 1}",
                    subfolder: widget.subfolder,
                  )));
    });
  }

  ValueNotifier<Offset> onTappedLocation = ValueNotifier(Offset.zero);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.moduleName),
      ),
      body: SingleChildScrollView(
        reverse: true,
        child: Container(
          decoration: const BoxDecoration(
              image: DecorationImage(
            image: AssetImage('assets/background1.png'),
            fit: BoxFit.cover,
          )),
          child: Stack(
            children: [
              GestureDetector(
                onTapDown: (details) {
                  onTappedLocation.value = details.localPosition;
                },
                child: CustomPaint(
                  painter: CoursePath(
                    onTappedLocation: onTappedLocation,
                    onTap: (int index) {
                      _navigateToLesson(index);
                    },
                  ),
                  size: const Size(double.infinity, 1900),
                ),
              ),
            ],
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
  }) : super(repaint: onTappedLocation);

  final ValueNotifier<Offset> onTappedLocation;
  final Function(int) onTap;
  final double circleRadius = 40;

  @override
  void paint(Canvas canvas, Size size) {
    Paint paint1 = Paint()
      ..color = const Color.fromRGBO(74, 91, 99, 1)
      ..strokeWidth = 12
      ..style = PaintingStyle.stroke;

    Paint paint2 = Paint()..color = const Color.fromRGBO(198, 219, 210, 1);

    Paint paint3 = Paint()
      ..color = const Color.fromRGBO(74, 91, 99, 1)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 12;

    var halfWidth = size.width / 2;

    final List<Offset> points1 = <Offset>[
      Offset(halfWidth, 4 * size.height / 5),
      Offset(160 - size.width / 4,
          ((4 * size.height / 5) + (3 * size.height / 5)) / 2),
      Offset(halfWidth, 3 * size.height / 5),
      Offset(40 + 3 * size.width / 4,
          ((3 * size.height / 5) + (2 * size.height / 5)) / 2),
      Offset(halfWidth, 2 * size.height / 5),
      Offset(160 - size.width / 4,
          ((2 * size.height / 5) + (size.height / 5)) / 2),
      Offset(halfWidth, size.height / 5),
      Offset(10 + 3 * size.width / 4, size.height / 10),
    ];

    final List<Offset> points2 = <Offset>[
      Offset(halfWidth, 4 * size.height / 5),
      Offset(
          size.width / 4, ((4 * size.height / 5) + (3 * size.height / 5)) / 2),
      Offset(halfWidth, 3 * size.height / 5),
      Offset(3 * size.width / 4,
          ((3 * size.height / 5) + (2 * size.height / 5)) / 2),
      Offset(halfWidth, 2 * size.height / 5),
      Offset(size.width / 4, ((2 * size.height / 5) + (size.height / 5)) / 2),
      Offset(halfWidth, size.height / 5),
      Offset(3 * size.width / 4, size.height / 10),
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

      ui.ParagraphBuilder paragraphBuilder =
          ui.ParagraphBuilder(paragraphStyle);
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
