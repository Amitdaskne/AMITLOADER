package com.privatesrcwala;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import com.privatesrcwala.utils.AppManager;

import org.lsposed.lsparanoid.Obfuscate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.CRC32;
import top.Vspace.blackbox.BlackBoxCore;
import top.Vspace.blackbox.core.env.BEnvironment;
import top.Vspace.blackbox.entity.pm.InstallResult;

@Obfuscate
public class MAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("privatesrcwala");
        } catch (UnsatisfiedLinkError ignored) {}
    }
    public static native String apkcrc(); 
    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final int USER_ID = 0;
    private static final int PICK_OBB_REQUEST = 999;

    private ProgressBar progressBar;
    private TextView progressText;
    private boolean isObbCopied = false;
    private AppManager appManager;
    
    private String selectedMode = "AUTO"; 

    public static native String exdate();

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        android.widget.FrameLayout contentHost = findViewById(android.R.id.content);
        contentHost.addView(new AnimatedBackgroundView(this), 0,
                new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(this, "Please allow All Files Access to copy OBB", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        getWindow().setStatusBarColor(getColor(R.color.background));
        appManager = new AppManager(this);
        doCountTimerAccout();

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        Button startBgmi = findViewById(R.id.startBgmi);
        startBgmi.setOnClickListener(v -> {
            showStartOptions();
        });

        TextView tvExitApp = findViewById(R.id.tvExitApp);
        tvExitApp.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        TextView toolsButton = findViewById(R.id.toolsButton);
        if (toolsButton != null) toolsButton.setOnClickListener(v -> showTools());
    }

    private void showTools() {
        String[] actions = {"Clear cache", "Check update", "App info"};
        new AlertDialog.Builder(this)
                .setTitle("FXC ANDROID")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) clearAppCache();
                    else if (which == 1) checkForUpdate();
                    else openAppInfo();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void clearAppCache() {
        long before = folderSize(getCacheDir());
        deleteContents(getCacheDir());
        Toast.makeText(this, "Cache cleared. Login data is safe.", Toast.LENGTH_SHORT).show();
        Log.d("FXC_TOOLS", "Cleared cache bytes: " + before);
    }

    private long folderSize(File file) {
        if (file == null || !file.exists()) return 0L;
        if (file.isFile()) return file.length();
        long total = 0L;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) total += folderSize(child);
        return total;
    }

    private void deleteContents(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] children = folder.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) deleteContents(child);
            if (!child.delete()) Log.w("FXC_TOOLS", "Could not delete cache item: " + child.getName());
        }
    }

    private void checkForUpdate() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            new AlertDialog.Builder(this)
                    .setTitle("FXC ANDROID")
                    .setMessage("Installed version: " + info.versionName + "\n\nYou are using the latest installed build. The update channel is ready to connect when a release URL is configured.")
                    .setPositiveButton("OK", null)
                    .show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Could not read app version.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "App info is unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStartOptions() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.FxcOptionsDialog)
                .setTitle("Start Options (" + selectedMode + ")")
                .setItems(new CharSequence[]{"Launch Game", "Clear Login Data"}, (optionsDialog, which) -> {
                    if (which == 0) {
                        handleLaunchFlow();
                    } else if (which == 1) {
                        handleClearLogin();
                    }
                })
                .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_card_bg);
                dialog.getWindow().setDimAmount(0.72f);
                dialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.82f),
                        android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            }

            int titleId = getResources().getIdentifier("alertTitle", "id", "android");
            TextView title = dialog.findViewById(titleId);
            if (title != null) {
                title.setTextColor(Color.WHITE);
                title.setTextSize(17f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setPadding(28, 22, 28, 10);
            }

            android.widget.ListView list = dialog.getListView();
            list.setBackgroundColor(Color.TRANSPARENT);
            list.setDivider(null);
            list.setDividerHeight(0);
            list.setPadding(12, 4, 12, 14);
            for (int i = 0; i < list.getChildCount(); i++) {
                View row = list.getChildAt(i);
                if (row instanceof TextView) {
                    TextView option = (TextView) row;
                    option.setTextColor(Color.rgb(11, 143, 168));
                    option.setTextSize(15f);
                    option.setPadding(18, 16, 18, 16);
                }
            }
        });
        dialog.show();
    }

    private void handleLaunchFlow() {
        if (!BlackBoxCore.get().isInstalled(PKG_BGMI, USER_ID)) {
            InstallResult result = BlackBoxCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
            if (result.success) {
                Toast.makeText(this, "BGMI installed inside container. Checking OBB...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Install failed: " + result.msg, Toast.LENGTH_SHORT).show();
                return; 
            }
        }

        try {
            PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
            String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
            File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
            File obbDestFile = new File(obbDestDir, gameObb);

            if (obbDestFile.exists() && obbDestFile.length() > 0) {
                isObbCopied = true;
                launchGame(); 
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Could not locate container package info.", Toast.LENGTH_SHORT).show();
        }

        copyObbFiles();
    }

    private void handleClearLogin() {
        try {
            BlackBoxCore.get().stopPackage(PKG_BGMI, USER_ID);
            BlackBoxCore.get().clearPackage(PKG_BGMI, USER_ID);
            Toast.makeText(this, "Login data cleared successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear login: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
   
    private void doCountTimerAccout() {
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expiryDate = dateFormat.parse(exdate());
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;

                    long d = distance / (24 * 60 * 60 * 1000);
                    long h = (distance / (60 * 60 * 1000)) % 24;
                    long m = (distance / (60 * 1000)) % 60;
                    long s = (distance / 1000) % 60;

                    if (distance < 0) {
                        Toast.makeText(MAct.this, "License expired!", Toast.LENGTH_SHORT).show();
                    } else {
                        ((TextView) findViewById(R.id.tvD)).setText(String.format("%02d", d));
                        ((TextView) findViewById(R.id.tvH)).setText(String.format("%02d", h));
                        ((TextView) findViewById(R.id.tvM)).setText(String.format("%02d", m));
                        ((TextView) findViewById(R.id.tvS)).setText(String.format("%02d", s));
                    }
                } catch (Exception ignored) {}
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void copyObbFiles() {
        runOnUiThread(() -> {
            findViewById(R.id.progressCard).setVisibility(android.view.View.VISIBLE);
            progressText.setText("Locating OBB file...");
            progressBar.setProgress(0);
        });

        new Thread(() -> {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";

                File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
                if (!obbDestDir.exists()) obbDestDir.mkdirs();
                File obbDestFile = new File(obbDestDir, gameObb);

                File obbSource = new File("/storage/emulated/0/Android/obb/" + PKG_BGMI + "/" + gameObb);

                if (!obbSource.exists() || !obbSource.canRead()) {
                    StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
                    for (StorageVolume storageVolume : sm.getStorageVolumes()) {
                        File obbDir;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            obbDir = storageVolume.getDirectory();
                        } else {
                            try {
                                Method getPathFile = StorageVolume.class.getMethod("getPathFile");
                                obbDir = (File) getPathFile.invoke(storageVolume);
                            } catch (Exception e) {
                                obbDir = null;
                            }
                        }

                        if (obbDir != null) {
                            File obbSubDir = new File(obbDir, "Android/obb/" + PKG_BGMI);
                            File obbFile = new File(obbSubDir, gameObb);
                            if (obbFile.exists() && obbFile.canRead()) {
                                obbSource = obbFile;
                                break;
                            }
                        }
                    }
                }

                if (obbSource == null || !obbSource.exists() || !obbSource.canRead()) {
                    runOnUiThread(() -> {
                        findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                        showObbFilePicker();
                    });
                    return;
                }

                if (obbDestFile.exists() && obbDestFile.length() == obbSource.length()) {
                    runOnUiThread(() -> {
                        isObbCopied = true;
                        findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                        launchGame();
                    });
                    return;
                }

                FileInputStream fis = new FileInputStream(obbSource);
                FileOutputStream fos = new FileOutputStream(obbDestFile);

                long total = obbSource.length();
                long copied = 0;
                byte[] buf = new byte[1024 * 64];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    copied += len;
                    int prog = (int) ((copied * 100) / total);
                    runOnUiThread(() -> {
                        progressBar.setProgress(prog);
                        progressText.setText("Copying OBB: " + prog + "%");
                    });
                }
                fis.close();
                fos.close();

                runOnUiThread(() -> {
                    isObbCopied = true;
                    findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                    // LOG CHECK: Aap Android Studio ke Logcat me dekh sakte hain ki file safe copy hui ya nahi
                    Log.d("ZYNOX_OBB", "Copied Path: " + obbDestFile.getAbsolutePath() + " | Size: " + obbDestFile.length());
                    launchGame();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "OBB copy failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showObbFilePicker() {
        new AlertDialog.Builder(this)
                .setTitle("OBB File Required")
                .setMessage("Android restrictions ki wajah se system automatic OBB read nahi kar paa raha hai.\n\n'SELECT OBB' par click karein aur storage se original BGMI ki OBB file ko select karein.")
                .setPositiveButton("SELECT OBB", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(Intent.createChooser(intent, "Select BGMI OBB File"), PICK_OBB_REQUEST);
                    } catch (Exception e) {
                        Toast.makeText(this, "File picker not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_OBB_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedObbUri = data.getData();
            copyObbFromUri(selectedObbUri);
        }
    }

    private void copyObbFromUri(Uri uri) {
        findViewById(R.id.progressCard).setVisibility(android.view.View.VISIBLE);
        progressText.setText("Preparing URI stream...");
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
                
                File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
                if (!obbDestDir.exists()) obbDestDir.mkdirs();
                File obbDestFile = new File(obbDestDir, gameObb);

                InputStream fis = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(obbDestFile);

                long total = 0;
                try {
                    android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1) total = cursor.getLong(sizeIndex);
                        cursor.close();
                    }
                } catch (Exception ignored) {}
                if (total <= 0) total = 2000000000L; 

                long copied = 0;
                byte[] buf = new byte[1024 * 64]; 
                int len;
                while ((len = fis.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    copied += len;
                    final long currentCopied = copied;
                    final long finalTotal = total;
                    runOnUiThread(() -> {
                        int prog = (int) ((currentCopied * 100) / finalTotal);
                        if (prog > 100) prog = 100;
                        progressBar.setProgress(prog);
                        progressText.setText("Copying via Stream: " + prog + "%");
                    });
                }
                fis.close();
                fos.close();

                runOnUiThread(() -> {
                    isObbCopied = true;
                    findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                    Log.d("ZYNOX_OBB", "URI Copied Path: " + obbDestFile.getAbsolutePath() + " | Size: " + obbDestFile.length());
                    launchGame();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressCard).setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Failed to copy selected OBB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void launchGame() {
        // Safe Main Thread Launch taaki background thread crash na kare
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Toast.makeText(this, "Launching via " + selectedMode + " mode...", Toast.LENGTH_SHORT).show();
                
                // NOTIFICATION / HINT: Agar aapko wo "OBB not found" wala text badalna hai, toh niche 
                // di gayi line ko uncomment karein (pehle apne xml me us textview ki exact ID check kar lena)
                // ((TextView) findViewById(R.id.YOUR_TEXTVIEW_ID_HERE)).setText("OBB Verified successfully");

                BlackBoxCore.get().launchApk(PKG_BGMI, USER_ID);
            } catch (Exception e) {
                Toast.makeText(this, "Launch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, 800); // 800ms delay taaki IO streams complete ho sakein safely
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
