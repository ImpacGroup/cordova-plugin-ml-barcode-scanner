//
//  BarcodeError.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 15.11.21.
//

import Foundation

enum BarcodeError: Error {
    case failedToScan
    case missingPermission
    case nothingFound
    case alreadyMarked
    case incompleteBarcode
    case invalidBarcode
    case couldNotDisplayError
}
