import 'package:flutter/material.dart';
import 'package:toastification/toastification.dart';

class ToastMessages {
  static void successToast(BuildContext context, String title) {
    toastification.show(
      context: context,
      type: ToastificationType.success,
      style: ToastificationStyle.flatColored,
      title: Text(title),
      alignment: Alignment.bottomCenter,
      autoCloseDuration: const Duration(seconds: 4),
      showProgressBar: false
    );
  }
  static void errorToast(BuildContext context, String title) {
    toastification.show(
      context: context,
      type: ToastificationType.error,
      style: ToastificationStyle.flatColored,
      title: Text(title),
      alignment: Alignment.bottomCenter,
      autoCloseDuration: const Duration(seconds: 4),
      showProgressBar: false
    );
  }
}