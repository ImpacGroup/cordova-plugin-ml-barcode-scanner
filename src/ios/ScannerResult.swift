//
//  ScannerResult.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 16.11.21.
//

import Foundation
import UIKit

@objc class ScannerResult: NSObject, Decodable {
    let title: String?
    let successImg: UIImage?
    let imgTintColor: UIColor?
    
    enum CodingKeys: String, CodingKey {
        case title
        case successImg
        case imgTintColor
    }
    
    required init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        title = try values.decode(String.self, forKey: .title)
        let imgTintString = try? values.decode(String.self, forKey: .imgTintColor)
        imgTintColor = imgTintString == nil ? UIColor.tintColor : UIColor.from(string: imgTintString)
        let imageString = try values.decode(String.self, forKey: .successImg)
        if let mImage = ScannerResult.imageForBase64String(imageString) {
            successImg = mImage.withRenderingMode(.alwaysOriginal)
        } else {
            successImg = UIImage(systemName: "checkmark.circle.fill")?.withRenderingMode(.alwaysTemplate)
        }
    }
    
    init(title: String) {
        self.title = title
        self.imgTintColor = .systemMint
        self.successImg = UIImage(systemName: "checkmark.circle.fill")?.withRenderingMode(.alwaysTemplate)
    }
    
    static func imageForBase64String(_ strBase64: String) -> UIImage? {
        do{
            let imageData = try Data(contentsOf: URL(string: strBase64)!)
            let image = UIImage(data: imageData)
            return image!
        }
        catch{
            return nil
        }
    }
}
