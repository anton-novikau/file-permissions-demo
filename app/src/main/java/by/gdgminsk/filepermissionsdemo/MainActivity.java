package by.gdgminsk.filepermissionsdemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import by.gdgminsk.filepermissionsdemo.util.FileUtils;
import by.gdgminsk.filepermissionsdemo.util.Prefs;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = "MainActivity";

    private static final String IMAGE_URL = "https://www.dropbox.com/s/vvukuhwfqqqoh2c/Emma%20Stone.jpg?dl=1";

    @BindView(R.id.image)
    ImageView mImage;
    @BindView(R.id.storage_switch)
    RadioGroup mStorageSwitch;
    @BindView(R.id.migration_switch)
    RadioGroup mMigrationSwitch;
    @BindView(R.id.load_image)
    Button mLoadButton;
    @BindView(R.id.migrate_image)
    Button mMigrateButton;

    private ImageHandlerKeeper mImageHandlerKeeper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mStorageSwitch.setOnCheckedChangeListener((group, checkedId) -> Prefs.get(MainActivity.this).setStorageMode(checkedId));
        mMigrationSwitch.setOnCheckedChangeListener((group, checkedId) -> Prefs.get(MainActivity.this).setMigrationMode(checkedId));

        Prefs prefs = Prefs.get(this);
        int storageMode = prefs.getStorageMode();
        mStorageSwitch.check(storageMode);
        mMigrationSwitch.check(prefs.getMigrationMode());

        boolean imageLoaded = prefs.isImageLoaded();

        setMigrationEnabled(imageLoaded && storageMode == R.id.storage_switch_common);
        setLoadEnabled(!imageLoaded);

        FragmentManager fm = getSupportFragmentManager();
        mImageHandlerKeeper = (ImageHandlerKeeper) fm.findFragmentByTag(ImageHandlerKeeper.FRAGMENT_TAG);
        if (mImageHandlerKeeper == null) {
            mImageHandlerKeeper = new ImageHandlerKeeper();
            fm.beginTransaction().add(mImageHandlerKeeper, ImageHandlerKeeper.FRAGMENT_TAG).commit();
        }

        if (mImageHandlerKeeper.getImageHandler() == null) {
            mImageHandlerKeeper.setImageHandler(new ImageHandler(getApplicationContext()));
        }

        if (imageLoaded) {
            loadImage(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reset:
                reset();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().remove(mImageHandlerKeeper).commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnClick(R.id.migrate_image)
    void onMigrateImageClicked() {
        MainActivityPermissionsDispatcher.migrateImageWithPermissionCheck(this);
    }

    @NeedsPermission(WRITE_EXTERNAL_STORAGE)
    void migrateImage() {
        File commonImageFile = FileUtils.getSaveFile(StorageMode.COMMON_STORAGE, this);
        if (commonImageFile == null || !commonImageFile.exists()) {
            Toast.makeText(this, "Nothing to migrate", Toast.LENGTH_SHORT).show();
            return; // early exit
        }

        File internalImageFile = FileUtils.getSaveFile(StorageMode.APPLICATION_STORAGE, this);
        if (internalImageFile == null) {
            Toast.makeText(this, "Destination is invalid", Toast.LENGTH_SHORT).show();
            return; // early exit
        }

        ImageHandler imageHandler = mImageHandlerKeeper.getImageHandler();
        if (imageHandler != null) {
            imageHandler.migrateImage(commonImageFile, internalImageFile, getMigrationMode());
        }
    }

    @OnClick(R.id.load_image)
    void onLoadImageClicked() {
        loadImage(false);
    }

    void loadImage(boolean fromCache) {
        Uri imageUri = Uri.parse(IMAGE_URL);
        @StorageMode
        int storageMode = getStorageMode();
        if (!fromCache && storageMode == StorageMode.COMMON_STORAGE) {
            MainActivityPermissionsDispatcher.loadImageWithPermissionCheck(this, imageUri, storageMode);
        } else {
            loadImage(imageUri, storageMode);
        }
    }

    @NeedsPermission(WRITE_EXTERNAL_STORAGE)
    void loadImage(Uri uri, @StorageMode int storageMode) {
        Log.d(LOG_TAG, "loadImage() called with: uri = [" + uri + "], storageMode = [" + storageMode + "]");
        ImageHandler imageHandler = mImageHandlerKeeper.getImageHandler();
        if (imageHandler != null) {
            imageHandler.loadImage(uri, storageMode);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageLoaded(ImageHandler.LoadResult result) {
        Log.d(LOG_TAG, "onImageLoaded() called with: result = [" + result + "]");
        Bitmap bitmap = result.bitmap;
        if (bitmap != null) {
            Prefs.get(this).setImageLoaded(true);
            setMigrationEnabled(getStorageMode() == StorageMode.COMMON_STORAGE);
            setLoadEnabled(false);
            mImage.setImageBitmap(bitmap);
        } else {
            mImage.setImageResource(R.drawable.avatar_default);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageMigrated(ImageHandler.MigrationResult result) {
        Log.d(LOG_TAG, "onImageMigrated() called with: result = [" + result + "]");
        Toast.makeText(this, "Migration done. Success =  " + result.success, Toast.LENGTH_SHORT).show();
        if (result.success) {
            mStorageSwitch.check(R.id.storage_switch_application);
            setMigrationEnabled(false);
        }
    }

    private void reset() {
        if (getStorageMode() == StorageMode.COMMON_STORAGE) {
            MainActivityPermissionsDispatcher.performResetWithPermissionCheck(this);
        } else {
            performReset();
        }
    }

    @NeedsPermission(WRITE_EXTERNAL_STORAGE)
    void performReset() {
        ImageHandler imageHandler = mImageHandlerKeeper.getImageHandler();
        if (imageHandler != null) {
            imageHandler.removeCached();
            cleanupState();
        }
    }

    @OnShowRationale(WRITE_EXTERNAL_STORAGE)
    void showStorageExplaination(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_explaination_title)
                .setMessage(R.string.permission_explaination_message_storage)
                .setPositiveButton(R.string.btn_allow, (dialog, which) -> request.proceed())
                .setNegativeButton(R.string.btn_deny, (dialog, which) -> request.cancel())
                .show();
    }

    @OnNeverAskAgain(WRITE_EXTERNAL_STORAGE)
    void showStorageExplainationNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_explaination_title)
                .setMessage(R.string.permission_never_ask_again_message_storage)
                .setPositiveButton(R.string.btn_settings, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_deny, null)
                .show();

    }

    private void cleanupState() {
        mImage.setImageResource(R.drawable.avatar_default);
        mStorageSwitch.check(R.id.storage_switch_common);
        mMigrationSwitch.check(R.id.migration_switch_java_move);
        setMigrationEnabled(false);
        setLoadEnabled(true);
        Prefs.get(this).reset();
    }

    private void setLoadEnabled(boolean enabled) {
        mLoadButton.setEnabled(enabled);
        for (int i = 0, size = mStorageSwitch.getChildCount(); i < size; i++) {
            mStorageSwitch.getChildAt(i).setEnabled(enabled);
        }
        mStorageSwitch.setEnabled(enabled);
    }

    private void setMigrationEnabled(boolean enabled) {
        mMigrateButton.setEnabled(enabled);
        for (int i = 0, size = mMigrationSwitch.getChildCount(); i < size; i++) {
            mMigrationSwitch.getChildAt(i).setEnabled(enabled);
        }
        mMigrationSwitch.setEnabled(enabled);
    }

    @StorageMode
    private int getStorageMode() {
        switch (mStorageSwitch.getCheckedRadioButtonId()) {
            case R.id.storage_switch_application:
                return StorageMode.APPLICATION_STORAGE;
            case R.id.storage_switch_common:
                return StorageMode.COMMON_STORAGE;
            default:
                return StorageMode.UNKNOWN;
        }
    }

    @MigartionMode
    private int getMigrationMode() {
        switch (mMigrationSwitch.getCheckedRadioButtonId()) {
            case R.id.migration_switch_java_move:
                return MigartionMode.JAVA_MOVE;
            case R.id.migration_switch_native_move:
                return MigartionMode.NATIVE_MOVE;
            case R.id.migration_switch_copy:
                return MigartionMode.COPY;
            default:
                return MigartionMode.UNKNOWN;
        }
    }
}
