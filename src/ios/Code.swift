//
//  Code.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 17.11.21.
//

import Foundation

@objc class Code: NSObject, Codable {
    let value: String
    let type: BarcodeType
    
    init(value: String, type: BarcodeType) {
        self.value = value
        self.type = type
        super.init()
    }
}
