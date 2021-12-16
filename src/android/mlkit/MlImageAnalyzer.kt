package de.impacgroup.mlbarcodescanner.module.mlkit

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class MlImageAnalyzer<T>(val processor: VisionImageProcessor<T>): ImageAnalysis.Analyzer {

    var listener: VisionImageProcessorListener<T>? = null
        set(value) {
            field = value
            value?.let { processor.setListener(it) }
        }

    override fun analyze(image: ImageProxy) {

        if (listener == null) {
            image.close()
            return
        }
        processor.processImageProxy(image)
    }
}