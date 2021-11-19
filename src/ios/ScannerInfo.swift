//
//  ScannerInfo.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 16.11.21.
//

import Foundation
import UIKit

@objc class ScannerInfo: NSObject, Decodable {
    let title: String?
    let infoText: String?
    let button: ScannerButton?
    
    init(title: String, text: String, button: ScannerButton) {
        self.title = title
        self.infoText = text
        self.button = button
        super.init()
    }
}

@objc class ScannerButton: NSObject, Decodable {
    let title: String
    let tintColor: UIColor
    let backgroundColor: UIColor
    let roundedCorners: Bool
    
    enum CodingKeys: String, CodingKey {
        case title
        case tintColor
        case backgroundColor
        case roundedCorners
    }
        
    required init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        title = try values.decode(String.self, forKey: .title)
        roundedCorners = try values.decode(Bool.self, forKey: .roundedCorners)
        tintColor = UIColor.from(string: try values.decode(String.self, forKey: .tintColor))
        backgroundColor = UIColor.from(string: try values.decode(String.self, forKey: .backgroundColor))
    }
    
    init(title: String, tintcolor: UIColor = UIColor.white, backgroundColor: UIColor = UIColor.red, roundedCorners: Bool = true) {
        self.title = title
        self.backgroundColor = backgroundColor
        self.roundedCorners = roundedCorners
        self.tintColor = tintcolor
        super.init()
    }        
}
