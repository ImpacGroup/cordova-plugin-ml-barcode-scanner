package de.impacgroup.mlbarcodescanner.module

import android.graphics.drawable.Drawable

data class ScannerResult(
    val title: String?,
    val successImg: Drawable?,
    val imgTintColor: String?
)
