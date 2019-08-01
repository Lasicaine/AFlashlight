package fi.suomaafrontieroy.aflashlight;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.snackbar.Snackbar;

import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_STOP = "STOP";
    private static final String CHANNEL_ID = "fi.suomaafrontieroy.aflashlight";
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private ImageView mButtonFlashLight;
    private NotificationManagerCompat mNotificationManager;
    private Boolean isLightOn = false;
    private Camera camera;
    private Camera.Parameters parameters;
    final int NOTIFICATION_ID = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            isLightOn = savedInstanceState.getBoolean("isLightOn");
        }

        mButtonFlashLight = findViewById(R.id.button_flashlight);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = getCameraId();
        } else {
            if (isFlashSupported()) {
                camera = Camera.open();
                parameters = camera.getParameters();
            }
        }
        onRestoreState();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("isLightOn", isLightOn);
    }

    public void onRestoreState() {
        if (mCameraId != null || isFlashSupported()) {
            setFlashlight(isLightOn);
        } else {
            mButtonFlashLight.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);

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
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isFlashSupported() {
        try {
            PackageManager pm = getPackageManager();
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @TargetApi(23)
    private boolean CheckWriteSettingsPermission() {
        return Settings.System.canWrite(this);
    }

    public void clickBtnUseFlash(View view) {
        setFlashlight(!isLightOn);
    }

    public void clickBtnUseScreen(View view) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (CheckWriteSettingsPermission()) {
                startScreenLight();
            } else {
                showNoWriteSettingsPermissionSnackbar();
            }
        } else startScreenLight();

    }

    private void setFlashlight(boolean enabled) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mCameraManager.setTorchMode(mCameraId, enabled);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (enabled) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                camera.setParameters(parameters);
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setButtonLightImage(enabled);
        if (enabled) {
            showNotification();
        } else {
            cancelNotification();
        }
        isLightOn = enabled;

    }

    private void startScreenLight() {
        Intent intent = new Intent(MainActivity.this, FullScreenBrightnessActivity.class);
        startActivity(intent);
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
                this, MainActivity.class);
        activityIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, activityIntent, 0);
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.cmd_turn_off_flashlight))
                .setTicker(getString(R.string.msg_flashlight_turn_on))
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

    public void showNoWriteSettingsPermissionSnackbar() {
        Snackbar.make(MainActivity.this.findViewById(R.id.activity_view), R.string.msg_explanation_write_settings_permission_needed, Snackbar.LENGTH_LONG)
                .setAction(R.string.btn_settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openApplicationSettings();

                        Toast.makeText(getApplicationContext(),
                                R.string.msg_allow_write_settings_permission,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setActionTextColor(getResources().getColor(R.color.colorAccent))
                .show();
    }

    @TargetApi(23)
    public void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
        appSettingsIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (CheckWriteSettingsPermission()) {
                startScreenLight();
            } else {
                Toast.makeText(getApplicationContext(),
                        R.string.msg_write_settings_permission_is_not_granted,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mCameraId != null) {
            setFlashlight(false);
        }
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) && isFlashSupported()) {
            camera.release();
        }
        super.onDestroy();
    }

}