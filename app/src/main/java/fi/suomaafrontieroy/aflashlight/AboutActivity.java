package fi.suomaafrontieroy.aflashlight;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.title_about_app);
        getVersionInfo();
    }

    private void getVersionInfo() {
        String version = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String result = getResources().getString(R.string.app_version) + " " + version;
        TextView textViewVersionInfo = (TextView) findViewById(R.id.txt_app_version);
        textViewVersionInfo.setText(result);
    }
}
