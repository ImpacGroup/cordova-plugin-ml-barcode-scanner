package de.impacgroup.mlbarcodescanner.module

import androidx.appcompat.app.AppCompatActivity

interface CameraScanner {
    fun startScanner(activity: AppCompatActivity, title: String, info: ScannerInfo)
}