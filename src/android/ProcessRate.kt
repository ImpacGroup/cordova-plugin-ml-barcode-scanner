package de.impacgroup.mlbarcodescanner.module

enum class ProcessRate(val interval: Int) {
    SLOW(5),
    MEDIUM(2),
    FAST(0)
}