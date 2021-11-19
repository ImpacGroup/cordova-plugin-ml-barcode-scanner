//
//  CameraViewControllerDelegate.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 09.11.21.
//

import Foundation

@objc protocol CameraViewControllerDelegate: AnyObject {
    
    func didSelect(_ code: Code)
    
    func isValid(_ code: Code, completion: @escaping (Bool) -> ())
    
    func setScreen() -> ScannerInfo
    
    func setResult(_ code: Code, completion: @escaping (ScannerResult) -> ())
    
    func preferredManuelInput()
    
    /**
     Gets called if the incoming image can not be analyzed.
     */
    @objc optional func couldNotAnalyzeInput()

    @objc optional func willClose()
    
    @objc optional func didClose()
}
