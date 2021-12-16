package de.impacgroup.mlbarcodescanner

enum class ScannerAction(val key: String) {
    IS_VALID("isValid"),
    RESULT_SCREEN("resultScreen"),
    DID_SELECT_CODE("didSelectCode"),
    MANUAL_INPUT("preferredManuelInput"),
    WILL_CLOSE("willClose"),
    DID_CLOSE("didClose"),
    UNKNOWN("unknown");

    companion object {
        fun getFor(name: String): ScannerAction {
            return when(name) {
                DID_CLOSE.key -> DID_CLOSE
                WILL_CLOSE.key -> WILL_CLOSE
                IS_VALID.key -> IS_VALID
                DID_SELECT_CODE.key -> DID_SELECT_CODE
                MANUAL_INPUT.key -> MANUAL_INPUT
                RESULT_SCREEN.key -> RESULT_SCREEN
                else -> UNKNOWN
            }
        }
    }
}