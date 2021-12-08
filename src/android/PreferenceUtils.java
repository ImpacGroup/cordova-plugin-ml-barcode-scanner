/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.impacgroup.mlbarcodescanner.module.mlkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.camera.core.CameraSelector;

import com.google.android.gms.common.images.Size;
import com.google.common.base.Preconditions;

import de.impacgroup.mlbarcodescanner.module.mlkit.CameraSource;

/** Utility class to retrieve shared preferences. */
public class PreferenceUtils {

  private static final String TAG = "PreferenceUtils";

  static void saveString(Context context, @StringRes int prefKeyId, @Nullable String value) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(context.getString(prefKeyId), value)
        .apply();
  }

  @Nullable
  public static CameraSource.SizePair getCameraPreviewSizePair(Context context, int cameraId) {
    Preconditions.checkArgument(
        cameraId == CameraSource.CAMERA_FACING_BACK
            || cameraId == CameraSource.CAMERA_FACING_FRONT);
    String previewSizePrefKey;
    String pictureSizePrefKey;
    if (cameraId == CameraSource.CAMERA_FACING_BACK) {
      previewSizePrefKey = "rcpvs";
      pictureSizePrefKey = "rcpts";
      try {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new CameraSource.SizePair(
                Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
      } catch (Exception e) {
        Log.e(TAG, "getCameraPreviewSizePair: ", e);
      }
    }


    return null;
  }

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @Nullable
  public static android.util.Size getCameraXTargetResolution(Context context, int lensfacing) {
    Preconditions.checkArgument(
        lensfacing == CameraSelector.LENS_FACING_BACK
            || lensfacing == CameraSelector.LENS_FACING_FRONT);
    String prefKey = "crctas";
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    try {
      return android.util.Size.parseSize(sharedPreferences.getString(prefKey, null));
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean shouldHideDetectionInfo(Context context) {
    return false;
  }

  public static boolean isCameraLiveViewportEnabled(Context context) {
    /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_camera_live_viewport);
    return sharedPreferences.getBoolean(prefKey, false);*/
    return false;
  }

  private PreferenceUtils() {}
}
