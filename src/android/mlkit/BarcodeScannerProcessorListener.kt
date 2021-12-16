package de.impacgroup.mlbarcodescanner.module.mlkit

import com.google.mlkit.vision.barcode.Barcode

interface BarcodeScannerProcessorListener: VisionImageProcessorListener<List<Barcode>> {
}