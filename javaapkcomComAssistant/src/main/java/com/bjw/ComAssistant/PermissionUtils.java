package com.bjw.ComAssistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    public static final int GOTO_SEETING_CODE = 152;
    /**
     * 判断是否有权限
     *
     * @param context
     * @param perms
     * @return
     */
    public static boolean hasPermissions(Context context, String... perms) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (context == null) {
            throw new IllegalArgumentException("Can't check permissions for null context");
        }

        for (String perm : perms) {
            if (context.checkSelfPermission( perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    /**
     * 申请权限
     */
    public static void requestPermissions(Activity activity, int requestCode, String[] permissions) {

        List<String> permissionList = new ArrayList<String>();
        for (String permission : permissions) {
            if (activity.getApplicationContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        String[] permissionsArray = permissionList.toArray(new String[permissionList.size()]);//将List转为数组
        if (permissionList.isEmpty()) {
            //不可能为空
        } else {
            activity.requestPermissions(permissionsArray, requestCode);
            //返回结果onRequestPermissionsResult
        }
    }

    /**
     * 申请权限的回调
     *
     * @param requestCode  请求权限时传入的请求码，用于区别是哪一次请求的
     * @param permissions  所请求的所有权限的数组
     * @param grantResults 权限授予结果，和 permissions 数组参数中的权限一一对应，元素值为两种情况，如下:
     *                     授予: PackageManager.PERMISSION_GRANTED
     *                     拒绝: PackageManager.PERMISSION_DENIED
     */
    public static void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                  int[] grantResults, PermissionCallbacks callBack) {
        //授予的权限。
        List<String> granted = new ArrayList<String>();

        //拒绝的权限
        List<String> denied = new ArrayList<String>();


        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm);
            } else {
                denied.add(perm);
            }
        }

        if (null != callBack) {
            if (denied.isEmpty()) {
                callBack.onPermissionsAllGranted(requestCode, granted, denied.isEmpty());
            }

            if (!denied.isEmpty()) {
                callBack.onPermissionsDenied(requestCode, denied);
            }
        }
    }
    /**
     * 用户是否拒绝权限，并检查“不要提醒”。
     *
     * @param activity
     * @param perms
     * @return
     */
    public static boolean somePermissionPermanentlyDenied(Activity activity,List<String> perms) {
        for (String deniedPermission : perms) {
            if (permissionPermanentlyDenied(activity, deniedPermission)) {
                return true;
            }
        }

        return false;
    }
    public static boolean permissionPermanentlyDenied(Activity activity, String perms) {
        if (!activity.shouldShowRequestPermissionRationale( perms)) {
            return true;
        }
        return false;
    }

    public static void showDialogGoToAppSettting(final Activity activity) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setMessage("去设置界面开启权限")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting(activity);
                    }
                }).setCancelable(false).show();
    }


    /**
     * 跳转到应用设置界面
     */
    public static void goToAppSetting(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, GOTO_SEETING_CODE);
    }

    public static void showPermissionReason(final int requestCode, final Activity activity, final String[] permission, String s) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setMessage(s)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(activity, requestCode, permission);
                    }
                })
                .setCancelable(false).show();
    }

    public interface PermissionCallbacks {

        /**
         * @param isAllGranted 是否全部同意
         */
        void onPermissionsAllGranted(int requestCode, List<String> perms, boolean isAllGranted);

        /**
         */
        void onPermissionsDenied(int requestCode, List<String> perms);

    }
}
