#include <jni.h>
#include <string>

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_by_gdgminsk_filepermissionsdemo_util_FileUtils_move(
            JNIEnv *env,
            jclass clazz,
            jstring from,
            jstring to) {
        const char *nativeFrom = env->GetStringUTFChars(from, JNI_FALSE);
        const char *nativeTo = env->GetStringUTFChars(to, JNI_FALSE);
        int status = rename(nativeFrom, nativeTo);

        env->ReleaseStringUTFChars(from, nativeFrom);
        env->ReleaseStringUTFChars(to, nativeTo);
        return (jboolean) (status == 0);
    }
}
