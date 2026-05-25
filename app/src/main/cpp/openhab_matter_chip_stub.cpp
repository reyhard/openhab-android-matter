#include <jni.h>

namespace {

void ThrowUnsupportedOperation(JNIEnv *env, const char *message) {
    jclass exceptionClass = env->FindClass("java/lang/UnsupportedOperationException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
    }
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeControllerMetadata(
        JNIEnv *env,
        jclass) {
    return env->NewStringUTF(
            "kind=stub;version=0.1.0;production=false;message=JNI bridge stub packaged without connectedhomeip controller");
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeCommissionBleThread(
        JNIEnv *env,
        jclass,
        jstring,
        jlong,
        jint) {
    ThrowUnsupportedOperation(env, "connectedhomeip commissioning is not implemented in the packaged JNI stub");
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeOpenCommissioningWindow(
        JNIEnv *env,
        jclass,
        jlong,
        jint,
        jint) {
    ThrowUnsupportedOperation(env, "connectedhomeip OpenCommissioningWindow is not implemented in the packaged JNI stub");
    return nullptr;
}
