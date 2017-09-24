package by.gdgminsk.filepermissionsdemo;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        StorageMode.UNKNOWN,
        StorageMode.COMMON_STORAGE,
        StorageMode.APPLICATION_STORAGE
})
public @interface StorageMode {
    int UNKNOWN = -1;
    int COMMON_STORAGE = 1;
    int APPLICATION_STORAGE = 2;
}
