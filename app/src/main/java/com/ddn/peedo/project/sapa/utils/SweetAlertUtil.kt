package com.ddn.peedo.project.sapa.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import cn.pedant.SweetAlert.SweetAlertDialog

object SweetAlertUtil {

    //    fun showLoading(
//        activity: Context,
//        title: String = "Loading",
//        message: String = "Please wait..."
//    ): SweetAlertDialog {
//        val dialog = SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE)
//        dialog.titleText = title
//        dialog.contentText = message
//        dialog.setCancelable(false)
//        dialog.show()
//        return dialog
//    }
    fun showLoading(
        activity: Context,
        title: String = "Loading",
        message: String = "Please wait..."
    ): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE) // back to PROGRESS_TYPE
        dialog.titleText = title
        dialog.contentText = message
        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

//    fun showSuccess(
//        activity: Context,
//        title: String,
//        message: String,
//        onConfirm: (() -> Unit)? = null
//    ) {
//        SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
//            .setTitleText(title)
//            .setContentText(message)
//            .setConfirmClickListener {
//                it.dismissWithAnimation()
//                onConfirm?.invoke()
//            }
//            .show()
//    }

    //fun showError(
//    activity: Context,
//    title: String,
//    message: String,
//    onConfirm: (() -> Unit)? = null
//) {
//    SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
//        .setTitleText(title)
//        .setContentText(message)
//        .setConfirmClickListener {
//            it.dismissWithAnimation()
//            onConfirm?.invoke()
//        }
//        .show()
//}
    fun showError(
        activity: Context,
        title: String,
        message: String,
        existing: SweetAlertDialog? = null,
        onConfirm: (() -> Unit)? = null
    ) {
        if (existing != null) {
            // ✅ Reuse the same dialog, just change its type — no stacking
            existing.changeAlertType(SweetAlertDialog.ERROR_TYPE)
            existing.titleText = title
            existing.contentText = message
            existing.setCancelable(true)
            existing.setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
        } else {
            SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(title)
                .setContentText(message)
                .setConfirmClickListener {
                    it.dismissWithAnimation()
                    onConfirm?.invoke()
                }
                .show()
        }
    }

    fun showSuccess(
        activity: Context,
        title: String,
        message: String,
        existing: SweetAlertDialog? = null,
        onConfirm: (() -> Unit)? = null
    ) {
        if (existing != null) {
            // ✅ Reuse the same dialog
            existing.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
            existing.titleText = title
            existing.contentText = message
            existing.setCancelable(true)
            existing.setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
        } else {
            SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(title)
                .setContentText(message)
                .setConfirmClickListener {
                    it.dismissWithAnimation()
                    onConfirm?.invoke()
                }
                .show()
        }
    }

    fun showInfo(
        activity: Context,
        title: String,
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        SweetAlertDialog(activity, SweetAlertDialog.NORMAL_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
            .show()
    }

    fun showWarning(
        activity: Context,
        title: String,
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
            .show()
    }

    fun showConfirm(
        activity: Context,
        title: String,
        message: String,
        confirmText: String = "Yes",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit
    ) {
        SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setCancelText(cancelText)
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm()
            }
            .setCancelClickListener {
                it.dismissWithAnimation()
            }
            .show()
    }

    fun showConfirm(
        activity: Context,
        title: String,
        message: String,
        confirmText: String = "Yes",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setCancelText(cancelText)
            .setConfirmClickListener(
                SweetAlertDialog.OnSweetClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    onConfirm()
                }
            )
            .setCancelClickListener(
                SweetAlertDialog.OnSweetClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    onCancel?.invoke()
                }
            )
            .show()
    }
}

