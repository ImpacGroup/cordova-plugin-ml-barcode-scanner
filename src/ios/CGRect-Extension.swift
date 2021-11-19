//
//  CGRect-Extension.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 18.11.21.
//

import UIKit

extension CGRect {
  /// Returns a `Bool` indicating whether the rectangle's values are valid`.
  func isValid() -> Bool {
    return
      !(origin.x.isNaN || origin.y.isNaN || width.isNaN || height.isNaN || width < 0 || height < 0
      || origin.x < 0 || origin.y < 0)
  }
}
