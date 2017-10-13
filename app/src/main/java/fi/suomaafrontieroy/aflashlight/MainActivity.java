package fi.suomaafrontieroy.aflashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import static android.support.v4.app.NotificationCompat.DEFAULT_VIBRATE;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_HIGH;

public class MainActivity extends Activity {

    private static final String ACTION_STOP = "STOP";
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private ImageView mButtonLight;
    private NotificationManagerCompat mNotificationManager;
    private Boolean isLightOn = false;
    final int NOTIFICATION_ID = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mButtonLight = (ImageView) findViewById(R.id.buttonLight);
        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mCameraId = getCameraId();
        if (savedInstanceState != null) {
            isLightOn = savedInstanceState.getBoolean("isLightOn");
        }

        if (mCameraId != null) {
            setFlashlight(isLightOn);
            mButtonLight.setEnabled(true);
        } else {
            mButtonLight.setEnabled(false);
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

    public void clickLight(View view) {
        setFlashlight(!isLightOn);
    }

    public void clickBtnUseScreen(View view) {
        Intent intent = new Intent(MainActivity.this, FullScreenBrightnessActivity.class);
        startActivityForResult(intent, 0);
    }

    private void setFlashlight(boolean enabled) {
        try {
            mCameraManager.setTorchMode(mCameraId, enabled);
            setButtonLightImage(enabled);
            if (enabled) {
                showNotification();
            } else {
                cancelNotification();
            }
            isLightOn = enabled;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setButtonLightImage(boolean enabled) {
        if (enabled) {
            mButtonLight.setImageResource(R.drawable.power_on_512);
        } else {
            mButtonLight.setImageResource(R.drawable.power_off_512);
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
