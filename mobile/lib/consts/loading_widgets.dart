import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mobile/consts/app_colours.dart';

class CustomLoadingWidgets {
  static Widget hourglass() {
    return const Center(
      child: SpinKitPouringHourGlassRefined(
        color: AppColours.textColor,
      ),
    );
  }
  static Widget spinningCircle() {
    return Center(
      child: SpinKitSpinningCircle(
        duration: const Duration(milliseconds: 3500),
        size: 100,
        itemBuilder: (BuildContext context, int index){
          return const Image(image: AssetImage('assets/logo.png'));
        },
      ),
    );
  }
  static Widget fourRotatingDots() {
    return LoadingAnimationWidget.fourRotatingDots(
      color: AppColours.textColor,
      size: 85,
    );
  }
}