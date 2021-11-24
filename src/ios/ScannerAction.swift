//
//  ScannerAction.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 24.11.21.
//

import Foundation

enum ScannerAction: String {
    case IS_VALID = "isValid"
    case RESULT_SCREEN = "resultScreen"
    case DID_SELECT_CODE = "didSelectCode"
    case MANUAL_INPUT = "preferredManuelInput"
    case WILL_CLOSE = "willClose"
    case DID_CLOSE = "didClose"
}
