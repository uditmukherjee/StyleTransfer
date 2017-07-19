#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <opencv2/stitching.hpp>
#include <android/log.h>
#include "utils/include/time_profiler.h"

#define  LOG_TAG "STITCHING"

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

using namespace std;
using namespace cv;

std::vector<Mat> image_parts;
Stitcher stitcher;
Ptr<detail::Blender> blender;

void init() {
    bool useWaveCorrection = true;
    bool useGPU = false;
    bool useMultiBand = false;

    stitcher = Stitcher::createDefault(useGPU);
    stitcher.setWaveCorrection(useWaveCorrection);

    if (useMultiBand) {
        blender = new detail::MultiBandBlender();
    } else {
        blender = new detail::FeatherBlender();
    }

    stitcher.setBlender(blender);

}

extern "C" JNIEXPORT void JNICALL
Java_jinxlabs_stylizedfilters_utils_ImageUtils_stitch(JNIEnv *env, jobject instance,
                                                     jobjectArray imgs, jlong resultMatAddr) {
    init();
    Mat& resultMat = *(Mat*) resultMatAddr;

    /* marshaling array of mats: http://stackoverflow.com/a/16111442 */
    jclass matClass = env->FindClass("org/opencv/core/Mat");
    jmethodID getNativeAddr = env->GetMethodID(matClass, "getNativeObjAddr", "()J");

    int numImgs = env->GetArrayLength(imgs);

    for (int i = 0; i < numImgs; ++i) {
        Mat img = *(Mat *) env->CallLongMethod(
                env->GetObjectArrayElement(imgs, i),
                getNativeAddr
        );

        transpose(img, img);
        flip(img, img, 0);

        if (img.channels() == 4) cvtColor(img, img, COLOR_RGBA2RGB);

        image_parts.push_back(img);
    }

    Mat result;
    Stitcher::Status status = stitcher.stitch(image_parts, result);

    if (status != Stitcher::OK) {
        if (status == Stitcher::ERR_NEED_MORE_IMGS) {
            // Do a plain image join here
            LOGE("STITCH_ERROR : Need more images");
        } else {
            LOGE("STITCH_ERROR Failed with error %d", status);
        }
    } else {
        transpose(result, result);
        flip(result, result, 1);
        result.copyTo(resultMat);
    }
}

