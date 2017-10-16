package fi.suomaafrontieroy.aflashlight;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.lang.annotation.Target;

import static android.support.v4.app.NotificationCompat.DEFAULT_VIBRATE;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_HIGH;

public class MainActivity extends Activity {

    private static final String ACTION_STOP = "STOP";
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private ImageView mButtonFlashLight;
    private NotificationManagerCompat mNotificationManager;
    private Boolean isLightOn = false;
    private Camera camera;
    private Camera.Parameters parameters;
    final int NOTIFICATION_ID = 23;
    private int SDK_VERSION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CheckWriteSettingsPermission();
            mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = getCameraId();
            SDK_VERSION = 1;
        } else {
            if (isFlashSupported()) {
                camera = Camera.open();
                parameters = camera.getParameters();
            } else {
                showNoFlashAlert();
            }
            SDK_VERSION = 2;
        }

        if (savedInstanceState != null) {
            isLightOn = savedInstanceState.getBoolean("isLightOn");
        }

        mButtonFlashLight = (ImageView) findViewById(R.id.buttonFlashLight);

        if (mCameraId != null) {
            setFlashlight(isLightOn);
        } else {
            mButtonFlashLight.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
        savedInstanceState.putBoolean("isLightOn", isLightOn);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_STOP.equals(intent.getAction())) {
            setFlashlight(false);
        }
    }

    @TargetApi(23)
    private String getCameraId() {
        try {
            String[] ids = mCameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facingDirection = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null && flashAvailable && facingDirection != null && facingDirection == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
            showNoFlashAlert();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isFlashSupported() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @TargetApi(23)
    private void CheckWriteSettingsPermission()
    {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
    }

    public void clickBtnUseFlash(View view) {
        setFlashlight(!isLightOn);
    }

    public void clickBtnUseScreen(View view) {
        Intent intent = new Intent(MainActivity.this, FullScreenBrightnessActivity.class);
        startActivityForResult(intent, 0);
    }

    private void setFlashlight(boolean enabled) {

        switch (SDK_VERSION) {
            case 1:
                try {
                    mCameraManager.setTorchMode(mCameraId, enabled);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                if (enabled) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    camera.startPreview();
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();}
                break;

        }
        setButtonLightImage(enabled);
        if (enabled) {
            showNotification();
        } else {
            cancelNotification();
        }
        isLightOn = enabled;

    }

    private void setButtonLightImage(boolean enabled) {
        if (enabled) {
            mButtonFlashLight.setImageResource(R.drawable.flashlight_on_512);
        } else {
            mButtonFlashLight.setImageResource(R.drawable.flashlight_off_512);
        }
    }

    private void showNotification() {
        Intent activityIntent = new Intent(
                this,MainActivity.class);
        activityIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,0,activityIntent,0);
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this)
        .setContentTitle("AFlashlight")
        .setContentText("Press to turn off the flashlight")
        .setTicker("Flashlight turn on")
        .setAutoCancel(true)
        .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{DEFAULT_VIBRATE})
                .setPriority(IMPORTANCE_HIGH);

        mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationManager.notify(NOTIFICATION_ID, nBuilder.build());
    }

    private void cancelNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void showNoFlashAlert() {
        new AlertDialog.Builder(this)
                .setMessage("Your device hardware does not support flashlight!")
                .setIcon(android.R.drawable.ic_dialog_alert).setTitle("Warning")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /*dialog.dismiss();
                        finish();*/
                    }
                }).show();
    }

    @Override
    protected void onDestroy() {
        if (mCameraId != null) {
            setFlashlight(false);
        }
        super.onDestroy();
    }

}
