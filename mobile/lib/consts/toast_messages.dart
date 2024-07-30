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
      showProgressBar: false,
    );
  }
  static void warningToast(BuildContext context, String title) {
    toastification.show(
      context: context,
      type: ToastificationType.warning,
      style: ToastificationStyle.flatColored,
      title: Text(title),
      alignment: Alignment.bottomCenter,
      autoCloseDuration: const Duration(seconds: 4),
      showProgressBar: false
    );
  }
  static void infoToast(BuildContext context, String title) {
    toastification.show(
      context: context,
      type: ToastificationType.info,
      style: ToastificationStyle.flatColored,
      title: Text(title),
      alignment: const Alignment(1.0, 0.8),
      autoCloseDuration: const Duration(seconds: 8),
      showProgressBar: false
    );
  }
}