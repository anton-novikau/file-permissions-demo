package by.gdgminsk.filepermissionsdemo;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        MigartionMode.UNKNOWN,
        MigartionMode.JAVA_MOVE,
        MigartionMode.NATIVE_MOVE,
        MigartionMode.COPY
})
public @interface MigartionMode {
    int UNKNOWN = -1;
    int JAVA_MOVE = 1;
    int NATIVE_MOVE = 2;
    int COPY = 3;
}
