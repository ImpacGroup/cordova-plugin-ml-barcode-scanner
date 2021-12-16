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
package de.impacgroup.mlbarcodescanner.module.mlkit

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.Throws
import com.google.mlkit.common.MlKitException
import androidx.camera.core.ImageProxy
import de.impacgroup.mlbarcodescanner.module.mlkit.VisionImageProcessorListener

/** An interface to process the images with different vision detectors and custom image models.  */
interface VisionImageProcessor<T> {

    /** Processes a bitmap image.  */
    fun processBitmap(bitmap: Bitmap?)

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case.  */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Throws(MlKitException::class)
    fun processImageProxy(image: ImageProxy)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()
    fun setListener(listener: VisionImageProcessorListener<T>)
}