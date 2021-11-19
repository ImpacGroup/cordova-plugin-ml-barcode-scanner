//
//  BarcodeType.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 15.11.21.
//

import Foundation
import MLKit

@objc enum BarcodeType: Int, Codable {
    case QR_CODE
    case EAN_13
    case EAN_8
    case Unknown
    
    static func from(format: BarcodeFormat) -> BarcodeType {
        switch format {
        case .EAN13:
                return EAN_13
        case .EAN8:
                return EAN_8
        case .qrCode:
                return QR_CODE
        default:
            return Unknown
        }
    }
}
