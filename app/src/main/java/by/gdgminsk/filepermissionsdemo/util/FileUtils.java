package by.gdgminsk.filepermissionsdemo.util;


import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import by.gdgminsk.filepermissionsdemo.StorageMode;

public final class FileUtils {
    static final String LOG_TAG = "FileUtils";

    private static final int BUFFER_SIZE = 4096;
    private static final int EOF = -1;

    static final String SAVE_DIR_NAME = "File Permissions";
    static final String SAVE_FILE_NAME = "avatar.jpg";

    static {
        System.loadLibrary("native-lib");
    }

    @Nullable
    public static File getSaveDir(@StorageMode int storageMode, String dirName, Context context) {
        switch (storageMode) {
            case StorageMode.COMMON_STORAGE:
                return getCommonDir(dirName);
            case StorageMode.APPLICATION_STORAGE:
                return getApplicationDir(context, dirName);
            case StorageMode.UNKNOWN: // fall through
            default:
                return null;
        }
    }

    @Nullable
    public static File getSaveFile(@StorageMode int storageMode, Context context) {
        File saveDir = getSaveDir(storageMode, SAVE_DIR_NAME, context);
        return saveDir != null ? new File(saveDir, SAVE_FILE_NAME) : null;
    }

    private static File getCommonDir(String dirName) {
        return new File(Environment.getExternalStorageDirectory(), dirName);
    }

    private static File getApplicationDir(Context context, String dirName) {
        return context.getExternalFilesDir(dirName);
    }

    public static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = from.read(buffer)) != EOF) {
            to.write(buffer, 0, len);
        }
    }

    public static void copy(File from, File to) throws IOException {
        if (to == null || from == null) {
            return;
        }
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(from).getChannel();
            outputChannel = new FileOutputStream(to).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            close(inputChannel, outputChannel);
        }
    }

    public static void close(Closeable... resources) {
        for (Closeable resource : resources) {
            if (resource == null) {
                continue;
            }

            try {
                resource.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to close resource", e);
            }
        }
    }

    public static native boolean move(String from, String to);
}
