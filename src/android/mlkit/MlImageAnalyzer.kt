package de.impacgroup.mlbarcodescanner.module.mlkit

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import de.impacgroup.mlbarcodescanner.module.ProcessRate

class MlImageAnalyzer<T>(val processor: VisionImageProcessor<T>): ImageAnalysis.Analyzer {

    /**
    The rate defines if every image or not every image should be processed. If processRate is set to FAST every image gets processed. MEDIUM is the default value.
     */
    var processRate: ProcessRate = ProcessRate.MEDIUM
    private var imageProcessCounter: Int = 0

    var listener: VisionImageProcessorListener<T>? = null
        set(value) {
            field = value
            value?.let { processor.setListener(it) }
        }

    override fun analyze(image: ImageProxy) {
        if (listener == null || !shouldProcessImage()) {
            image.close()
            return
        }
        processor.processImageProxy(image)
    }

    private fun shouldProcessImage(): Boolean {
        if (imageProcessCounter >= processRate.interval) {
            imageProcessCounter = 0
            return true
        } else {
            imageProcessCounter += 1
            return false
        }
    }
}