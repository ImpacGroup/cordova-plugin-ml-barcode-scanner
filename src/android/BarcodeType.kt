package de.impacgroup.mlbarcodescanner.module

import com.google.mlkit.vision.barcode.Barcode

enum class BarcodeType {
    QR_CODE,
    EAN_13,
    EAN_8,
    Unknown;

    companion object {
        fun from(barcode: Barcode): BarcodeType {
            return when(barcode.format) {
                Barcode.FORMAT_EAN_13 -> EAN_13
                Barcode.FORMAT_EAN_8 -> EAN_8
                Barcode.FORMAT_QR_CODE -> QR_CODE
                else -> Unknown
            }
        }
    }

}