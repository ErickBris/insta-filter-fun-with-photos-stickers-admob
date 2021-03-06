package com.stickercamera.base;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import com.stickercamera.base.util.DialogHelper;


class ActivityHelper {
    final static String TAG = ActivityHelper.class.getSimpleName();

    /**
     * 对应的Activity
     */
    private Activity mActivity;


    /**
     * 对话框帮助类
     */
    private DialogHelper mDialogHelper;

    ActivityHelper(Activity activity) {
        mActivity = activity;
        mDialogHelper = new DialogHelper(mActivity);
    }

    public void finish() {
        mDialogHelper.dismissProgressDialog();
    }

    /**
     * 弹对话框
     *
     * @param title            标题
     * @param msg              消息
     * @param positive         确定
     * @param positiveListener 确定回调
     * @param negative         否定
     * @param negativeListener 否定回调
     */
    void alert(String title, String msg, String positive,
               DialogInterface.OnClickListener positiveListener, String negative,
               DialogInterface.OnClickListener negativeListener) {
        mDialogHelper.alert(title, msg, positive, positiveListener, negative, negativeListener);
    }


    /**
     * 弹对话框
     *
     * @param title                    标题
     * @param msg                      消息
     * @param positive                 确定
     * @param positiveListener         确定回调
     * @param negative                 否定
     * @param negativeListener         否定回调
     * @param isCanceledOnTouchOutside 外部是否可点取消
     */
    void alert(String title, String msg, String positive,
               DialogInterface.OnClickListener positiveListener, String negative,
               DialogInterface.OnClickListener negativeListener,
               Boolean isCanceledOnTouchOutside) {
        mDialogHelper.alert(title, msg, positive, positiveListener, negative, negativeListener,
                isCanceledOnTouchOutside);
    }

    /**
     * TOAST
     *
     * @param msg    消息
     * @param period 时长
     */
    void toast(String msg, int period) {
        mDialogHelper.toast(msg, period);
    }

    /**
     * 显示进度对话框
     *
     * @param msg 消息
     */
    void showProgressDialog(String msg) {
        mDialogHelper.showProgressDialog(msg);
    }

    /**
     * 显示可取消的进度对话框
     *
     * @param msg 消息
     */
     void showProgressDialog(final String msg, final boolean cancelable,
                                   final OnCancelListener cancelListener) {
        mDialogHelper.showProgressDialog(msg, cancelable, cancelListener, true);
    }

     void dismissProgressDialog() {
        mDialogHelper.dismissProgressDialog();
    }
}
