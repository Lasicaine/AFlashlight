package fi.suomaafrontieroy.aflashlight;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_MAX;

public class MainActivity extends Activity {

    private static final String ACTION_STOP = "STOP";
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private ImageView mButtonLight;
    private Boolean lightOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mButtonLight = (ImageView) findViewById(R.id.buttonLight);

        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mCameraId = getCameraId();
        mButtonLight.setImageResource(R.drawable.power_off_512);
        if (mCameraId != null) {
            mButtonLight.setEnabled(true);
            lightOn = false;
        } else {
            mButtonLight.setEnabled(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_STOP.equals(intent.getAction())) {
            lightOn = false;
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
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clickLight(View view) {

        if (lightOn) {
            lightOn = false;
            NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        } else {
            lightOn = true;
            showNotification();
        }

        setFlashlight(lightOn);
    }

    private void setFlashlight(boolean enabled) {
        try {
            mCameraManager.setTorchMode(mCameraId, enabled);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
        final Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle("Flashlight")
                .setContentText("Press to turn off the flashlight")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setLargeIcon(BitmapFactory.decodeResource(
                                        getResources(), R.mipmap.ic_launcher))
                                .setContentIntent(pendingIntent)
                                .setVibrate(new long[]{DEFAULT_VIBRATE})
                                .setPriority(PRIORITY_MAX);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0,
                notificationBuilder.build());
    }
}
