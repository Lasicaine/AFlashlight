package fi.suomaafrontieroy.aflashlight;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.title_about_app);
        setVersionInfo();
    }

    private void setVersionInfo() {
        TextView textViewVersionInfo = findViewById(R.id.txt_app_version);
        textViewVersionInfo.setText(getResources().getString(R.string.app_version, BuildConfig.VERSION_NAME));
    }
}
