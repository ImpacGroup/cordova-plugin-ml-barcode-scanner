package de.impacgroup.mlbarcodescanner.module

enum class ProcessRate(val interval: Int) {
    SLOW(10),
    MEDIUM(4),
    FAST(0)
}