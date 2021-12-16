package de.impacgroup.mlbarcodescanner.module.mlkit

import com.google.mlkit.vision.barcode.Barcode

interface VisionImageProcessorListener<T> {
    fun onSuccess(result: T)
}