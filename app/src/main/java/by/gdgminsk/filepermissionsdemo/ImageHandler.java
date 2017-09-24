package by.gdgminsk.filepermissionsdemo;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import by.gdgminsk.filepermissionsdemo.util.FileUtils;
import by.gdgminsk.filepermissionsdemo.util.Prefs;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class ImageHandler {
    static final String LOG_TAG = "ImageHandler";

    private final Context mContext;
    private final HandlerThread mWorkerThread;
    @Nullable
    private Handler mWorker;

    private final Queue<Runnable> mPendingTasks = new LinkedList<>();

    public ImageHandler(Context context) {
        mContext = context;
        mWorkerThread = new HandlerThread("Image Worker") {
            @Override
            protected void onLooperPrepared() {
                mWorker = new Handler(getLooper());
                Runnable task;
                while ((task = mPendingTasks.poll()) != null) {
                    mWorker.post(task);
                }
            }
        };
        mWorkerThread.start();
    }

    public void removeCached() {
        postToWorker(() -> {
            File internalFile = FileUtils.getSaveFile(StorageMode.APPLICATION_STORAGE, mContext);
            File externalFile = FileUtils.getSaveFile(StorageMode.COMMON_STORAGE, mContext);
            boolean internalRemoved = !(internalFile != null && internalFile.exists()) || internalFile.delete();
            boolean externalRemoved = !(externalFile != null && externalFile.exists()) || externalFile.delete();

            Log.d(LOG_TAG, "removeCached(): internal file removed = " + internalRemoved +
                    ", external file removed = " + externalRemoved);
        });
    }

    public void loadImage(@NonNull final Uri imageUri, @StorageMode final int storageMode) {
        postToWorker(() -> {
            File imageFile = FileUtils.getSaveFile(storageMode, mContext);
            if (imageFile == null) {
                Log.w(LOG_TAG, "loadImage(): unsupported storage mode = " + storageMode);
                return; // early exit
            }
            if (!Prefs.get(mContext).isImageLoaded()) {
                downloadImage(imageUri, imageFile);
            }
            Bitmap bitmap = loadCachedImage(imageFile);
            EventBus.getDefault().post(new LoadResult(bitmap));
        });
    }

    public void migrateImage(@NonNull final File from, @NonNull final File to, @MigartionMode final int migrationMode) {
        postToWorker(() -> {
            boolean success = false;
            switch (migrationMode) {
                case MigartionMode.JAVA_MOVE:
                    success = from.renameTo(to);
                    break;
                case MigartionMode.NATIVE_MOVE:
                    success = FileUtils.move(from.getAbsolutePath(), to.getAbsolutePath());
                    break;
                case MigartionMode.COPY:
                    try {
                        FileUtils.copy(from, to);
                        success = from.delete();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "migrateImage(): unable to copy file " + from + " to " + to, e);
                    }
                    break;
                case MigartionMode.UNKNOWN: // fall through
                default:
                    success = false;
                    break;
            }
            EventBus.getDefault().post(new MigrationResult(success));
        });
    }

    private void postToWorker(Runnable action) {
        if (mWorker == null) {
            mPendingTasks.add(action);
            return; // early exit
        }

        mWorker.post(action);
    }

    private static Bitmap loadCachedImage(@NonNull File imageFile) {
        if (!imageFile.exists()) {
            return null;
        }

        Bitmap bitmap = null;
        InputStream stream = null;
        try {
            stream = new FileInputStream(imageFile);
            bitmap = BitmapFactory.decodeStream(stream);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "loadCachedImage(): unable to load image", e);
        } finally {
            FileUtils.close(stream);
        }
        return bitmap;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean downloadImage(@NonNull Uri imageUri, @NonNull File saveFile) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUri.toString())
                .build();
        InputStream in = null;
        OutputStream out = null;
        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return false;
            }

            File saveDir = saveFile.getParentFile();
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            in = body.byteStream();
            out = new FileOutputStream(saveFile);
            FileUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to load image", e);
        } finally {
            FileUtils.close(in, out);
        }
        return false;
    }

    public static class LoadResult {
        @Nullable
        public final Bitmap bitmap;

        LoadResult(@Nullable Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public String toString() {
            return "LoadResult{" +
                    "bitmap=" + bitmap +
                    '}';
        }
    }

    public static class MigrationResult {
        public final boolean success;

        MigrationResult(boolean success) {
            this.success = success;
        }

        @Override
        public String toString() {
            return "MigrationResult{" +
                    "success=" + success +
                    '}';
        }
    }
}
