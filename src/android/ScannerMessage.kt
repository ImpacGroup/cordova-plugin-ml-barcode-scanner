package de.impacgroup.mlbarcodescanner

import com.google.gson.annotations.SerializedName

data class ScannerMessage<T>(
    val action: String,
    @SerializedName("object") val _object : T?
)
