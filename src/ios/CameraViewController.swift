//
//  CameraViewController.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 09.11.21.
//

import UIKit
import AVFoundation
import MLKit
import Combine


class CameraViewController: UIViewController {
    
    private var previewLayer: AVCaptureVideoPreviewLayer!
    private lazy var captureSession = AVCaptureSession()
    private lazy var sessionQueue = DispatchQueue(label: "de.impacgroup.cordova.MLBarcodeScanner.SessionQueue")
    private var lastFrame: CMSampleBuffer?
    private var cancellables = Set<AnyCancellable>()
    private var detectedBarcodes: [Barcode] = []
    
    public weak var delegate: CameraViewControllerDelegate? = nil
    
    /**
     The rate defines if every image or not every image should be processed. If processRate is set to FAST every image gets processed. MEDIUM is the default value.
     */
    public var processRate: ProcessRate = .MEDIUM
    private var imageProcessCounter: Int = 0
    
    @IBOutlet private weak var cameraView: UIView!
    @IBOutlet weak var scanView: UIView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var bottomView: UIView!
    
    /**
     Info view to display information for the user during scan process.
     */
    @IBOutlet weak var infoView: UIView!
    @IBOutlet weak var infoDescriptionTextView: UITextView!
    @IBOutlet weak var infoLabel: UILabel!
    @IBOutlet weak var infoButton: UIButton!
    
    /**
     Result view to display the detected barcode to the user.
     */
    @IBOutlet weak var resultView: UIView!
    @IBOutlet weak var resultLabel: UILabel!
    @IBOutlet weak var resultImageView: UIImageView!
    
    
    private lazy var previewOverlayView: UIImageView = {
        precondition(isViewLoaded)
        let previewOverlayView = UIImageView(frame: .zero)
        previewOverlayView.contentMode = UIView.ContentMode.scaleAspectFill
        previewOverlayView.translatesAutoresizingMaskIntoConstraints = false
        return previewOverlayView
    }()
    
    private var scanFrame: CGRect = CGRect(x: 0,y: 0,width: 1,height: 1)
    private var videoFrame: CGRect = CGRect(x: 0, y: 0, width: 1, height: 1)

    override func viewDidLoad() {
        super.viewDidLoad()
        previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        titleLabel.text = title
        setUpPreviewOverlayView()
        setupCaptureSession()
        setupInfoView()
        // Do any additional setup after loading the view.
    }
            
    private func setupCaptureSession() {
        weak var weakSelf = self
        guard let strongSelf = weakSelf else {
            return
        }
        
        strongSelf.setUpCaptureSessionOutput()
        .flatMap { _ in
            return strongSelf.setUpCaptureSessionInput()
        }
        .receive(on: DispatchQueue.main)
        .map { (_) -> Bool in
            return true
        }
        .catch { error -> AnyPublisher<Bool, Never> in
            if let mError = error as? BarcodeError, mError == .missingPermission {
                print("Missing Permission")
                if let msg = strongSelf.delegate?.permissionErrorMsg?() {
                    strongSelf.display(info: msg)
                } else {
                    strongSelf.delegate?.preferredManuelInput()
                    strongSelf.close()
                }
            }
            return Just(false).eraseToAnyPublisher()
        }.sink { result in
            if result {
                strongSelf.setScanArea()
                strongSelf.startSession()
            }
        }.store(in: &cancellables)
    }
    
    private func setScanArea() {
        scanFrame = scanView.frame
        videoFrame = cameraView.frame
    }
    
    override func viewDidDisappear(_ animated: Bool) {
      super.viewDidDisappear(animated)
      stopSession()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer.frame = cameraView.frame
    }
    
    private func startSession() {
      weak var weakSelf = self
      sessionQueue.async {
        guard let strongSelf = weakSelf else {
          print("Self is nil!")
          return
        }
        strongSelf.captureSession.startRunning()
      }
    }
    
    private func stopSession() {
      weak var weakSelf = self
      sessionQueue.async {
        guard let strongSelf = weakSelf else {
          print("Self is nil!")
          return
        }
        strongSelf.captureSession.stopRunning()
      }
    }
    
    private func setUpCaptureSessionOutput() -> AnyPublisher<Void, Error> {
        return Future<Void, Error> { [weak self] promise in
            guard let strongSelf = self else {
                print("Self is nil!")
                promise(Result.failure(BarcodeError.failedToScan))
                return
            }
            strongSelf.sessionQueue.async {
                strongSelf.captureSession.beginConfiguration()
                // When performing latency tests to determine ideal capture settings,
                // run the app in 'release' mode to get accurate performance metrics
                strongSelf.captureSession.sessionPreset = AVCaptureSession.Preset.hd1920x1080
                                      
                let output = AVCaptureVideoDataOutput()
                output.videoSettings = [
                  (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA
                ]
                output.alwaysDiscardsLateVideoFrames = true
                  
                let outputQueue = DispatchQueue(label: Constant.videoDataOutputQueueLabel)
                output.setSampleBufferDelegate(strongSelf, queue: outputQueue)
                guard strongSelf.captureSession.canAddOutput(output) else {
                    print("Failed to add capture session output.")
                    strongSelf.captureSession.commitConfiguration()
                    promise(Result.failure(BarcodeError.failedToScan))
                    return
                }
                strongSelf.captureSession.addOutput(output)
                strongSelf.captureSession.commitConfiguration()
                promise(Result.success(Void()))
            }
        }.eraseToAnyPublisher()
    }

    private func setUpCaptureSessionInput() -> AnyPublisher<Void, Error> {
        return Future<Void, Error> { [weak self] promise in
            guard let strongSelf = self else {
                promise(Result.failure(BarcodeError.failedToScan))
                return
            }
            strongSelf.sessionQueue.async {
                
                let cameraPosition: AVCaptureDevice.Position = .back
                guard let device = strongSelf.captureDevice(forPosition: cameraPosition) else {
                    print("Failed to get capture device for camera position: \(cameraPosition)")
                    promise(Result.failure(BarcodeError.failedToScan))
                    return
                }
                do {
                    strongSelf.captureSession.beginConfiguration()
                    let currentInputs = strongSelf.captureSession.inputs
                    for input in currentInputs {
                        strongSelf.captureSession.removeInput(input)
                    }
                    let input = try AVCaptureDeviceInput(device: device)
                
                    try device.lockForConfiguration()
                    device.activeVideoMinFrameDuration = CMTime(value: 1, timescale: 25)
                    device.activeVideoMaxFrameDuration = CMTime(value: 1, timescale: 30)
                    device.unlockForConfiguration()
                    
                    guard strongSelf.captureSession.canAddInput(input) else {
                        print("Failed to add capture session input.")
                        promise(Result.failure(BarcodeError.failedToScan))
                        strongSelf.captureSession.commitConfiguration()
                        return
                    }
                    strongSelf.captureSession.addInput(input)
                    strongSelf.captureSession.commitConfiguration()
                    promise(Result.success(Void()))
                } catch {
                    promise(Result.failure(BarcodeError.missingPermission))
                    strongSelf.captureSession.commitConfiguration()
                    print("Failed to create capture device input: \(error.localizedDescription)")
                }
            }
        }.eraseToAnyPublisher()
    }
    
    private func captureDevice(forPosition position: AVCaptureDevice.Position) -> AVCaptureDevice? {
      if #available(iOS 10.0, *) {
        let discoverySession = AVCaptureDevice.DiscoverySession(
          deviceTypes: [.builtInWideAngleCamera],
          mediaType: .video,
          position: .unspecified
        )
        return discoverySession.devices.first { $0.position == position }
      }
      return nil
    }
    
    private func updatePreviewOverlayViewWithLastFrame() {
        weak var weakSelf = self
        DispatchQueue.main.sync {
            guard let strongSelf = weakSelf else {
                print("Self is nil!")
                return
            }

            guard let lastFrame = lastFrame,
                  let imageBuffer = CMSampleBufferGetImageBuffer(lastFrame)
            else {
                return
            }
            strongSelf.updatePreviewOverlayViewWithImageBuffer(imageBuffer)
        }
    }
    
    private func setUpPreviewOverlayView() {
      cameraView.addSubview(previewOverlayView)
      NSLayoutConstraint.activate([
        previewOverlayView.topAnchor.constraint(equalTo: cameraView.topAnchor),
        previewOverlayView.bottomAnchor.constraint(equalTo: cameraView.bottomAnchor),
        previewOverlayView.leadingAnchor.constraint(equalTo: cameraView.leadingAnchor),
        previewOverlayView.trailingAnchor.constraint(equalTo: cameraView.trailingAnchor),

      ])
    }
    
    private func setupInfoView() {
        bottomView.layer.cornerRadius = 8
        if let info = delegate?.setScreen() {
            display(info: info)
        } else {
            infoView.isHidden = true
        }
    }
    
    private func display(info: ScannerInfo) {
        infoView.isHidden = false
        infoLabel.text = info.title
        infoDescriptionTextView.text = info.infoText
        if let button = info.button {
            infoButton.isHidden = false
            infoButton.setTitle(button.title, for: .normal)
            infoButton.tintColor = button.tintColor
            infoButton.backgroundColor = button.backgroundColor
            infoButton.layer.cornerRadius = button.roundedCorners ? 8 : 0
        } else {
            infoView.isHidden = true
        }
    }
    
    private func updateResultView(barcode: Barcode, animated: Bool = true, completion: @escaping () -> ()) {
        delegate?.setResult(Code(value: barcode.displayValue!, type: BarcodeType.from(format: barcode.format)), completion: { [weak self] result in
            DispatchQueue.main.async {
                guard let strongSelf = self else {
                    return
                }
                strongSelf.resultLabel.text = result.title
                strongSelf.resultImageView.image = result.successImg
                strongSelf.resultImageView.tintColor = result.imgTintColor
                
                if animated {
                    strongSelf.resultView.alpha = 0.0
                    strongSelf.resultView.isHidden = false
                    strongSelf.resultImageView.transform = CGAffineTransform(scaleX: 0.0, y: 0.0)
                    UIView.animate(withDuration: 0.3, delay: 0.0, options: .curveEaseInOut) {
                        strongSelf.infoView.alpha = 0.0
                        strongSelf.resultView.alpha = 1.0
                    } completion: { [weak self] _ in
                        strongSelf.infoView.alpha = 1.0
                        UIView.animate(withDuration: 0.7, delay: 0.0, usingSpringWithDamping: 0.4, initialSpringVelocity: 0, options: .curveEaseInOut, animations: {
                            strongSelf.resultImageView.transform = .identity
                        }) { _ in
                            completion()
                        }
                    }
                } else {
                    strongSelf.resultView.isHidden = false
                    completion()
                }
            }
        })
    }
    
    private func getImageOrientation() -> UIImage.Orientation {
        if let interfaceOrientation = UIApplication.shared.windows.first?.windowScene?.interfaceOrientation {
            switch interfaceOrientation {
            case .unknown:
                return .right
            case .portrait:
                return .right
            case .portraitUpsideDown:
                return .left
            case .landscapeLeft:
                return .down
            case .landscapeRight:
                return .up
            @unknown default:
                return .right
            }
        } else {
            switch UIDevice.current.orientation {
            case .landscapeLeft:
                return .up
            case .landscapeRight:
                return .down
            case .portrait:
                return .right
            case .portraitUpsideDown:
                return .left
            default:
                return .right
            }
        }
    }
    
    private func updatePreviewOverlayViewWithImageBuffer(_ imageBuffer: CVImageBuffer?) {
        guard let imageBuffer = imageBuffer else {
            return
        }
        let orientation: UIImage.Orientation = getImageOrientation()
        let image = UIUtilities.createUIImage(from: imageBuffer, orientation: orientation)
        previewOverlayView.image = image
    }
    
    @IBAction func infoBtnPressed(_ sender: Any) {
        delegate?.preferredManuelInput()
        close()
    }
    
    @IBAction func closeBtnPressed(_ sender: Any) {
        close()
    }
    
    private func close() {
        delegate?.willClose?()
        dismiss(animated: true) { [weak self] in
            self?.delegate?.didClose?()
        }
    }
}

extension CameraViewController: AVCaptureVideoDataOutputSampleBufferDelegate {
    
    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            print("Failed to get image buffer from sample buffer.")
            return
        }

        lastFrame = sampleBuffer
        updatePreviewOverlayViewWithLastFrame()
        
        if shouldProcessImage() {
            guard let mlImage = getMLImage(sampleBuffer: sampleBuffer, cropArea: scanFrame, videoFrame: videoFrame) else {
                DispatchQueue.main.sync { [weak self] in
                    self?.delegate?.couldNotAnalyzeInput?()
                }
                return
            }
            
            let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
            let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))
            analyzeOnDevice(in: mlImage, width: imageWidth, height: imageHeight)
        }
    }
    
    private func shouldProcessImage() -> Bool {
        if imageProcessCounter >= processRate.rawValue {
            imageProcessCounter = 0
            return true
        } else {
            imageProcessCounter += 1
            return false
        }
    }
    
    /**
     Creates a VisionImage which size and position is simular to the cropArea Rect.
     */
    private func getMLImage(sampleBuffer: CMSampleBuffer, cropArea: CGRect, videoFrame: CGRect) -> UIImage? {
        let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)!

        CVPixelBufferLockBaseAddress(imageBuffer, .readOnly)
        let baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)
        let bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer)
        let width = CVPixelBufferGetWidth(imageBuffer)
        let height = CVPixelBufferGetHeight(imageBuffer)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue)

        let context = CGContext(data: baseAddress, width: width, height: height, bitsPerComponent: 8, bytesPerRow: bytesPerRow, space: colorSpace, bitmapInfo: bitmapInfo.rawValue)
        
        guard let cgImage: CGImage = context?.makeImage() else {
            print("Could not create vision image.")
            return nil
        }
        CVPixelBufferUnlockBaseAddress(imageBuffer, .readOnly)
        
        let scaleFactor = CGFloat(width) / videoFrame.height
        
        let cropedRect = CGRect(x: cropArea.origin.y * scaleFactor, y: (CGFloat(height) - videoFrame.width * scaleFactor) / 2, width: cropArea.size.height * scaleFactor, height: cropArea.size.width  * scaleFactor)
        let cropedCGImage = cgImage.cropping(to: cropedRect)
                            
        return UIImage(cgImage: cropedCGImage!, scale: 1, orientation: .right)
    }
    
    /**
     Analyzes if the image contains a barcode. If one is detected it highlights the barcode and informs the delegate.
     */
    private func analyzeOnDevice(in image: UIImage, width: CGFloat, height: CGFloat) {
        
        weak var weakSelf = self
        guard let strongSelf = weakSelf else {
            return
        }
        
        getVisionImage(image)
            .setFailureType(to: Error.self)
            .flatMap ({ visionImage -> AnyPublisher<[Barcode], Error> in
                return strongSelf.scanForBarcodeIn(visionImage)
            })
            .flatMap { codes in
                return strongSelf.validate(codes: codes)
            }.catch { error -> AnyPublisher<[Barcode], Error> in
                strongSelf.detectedBarcodes = []
                DispatchQueue.main.async {
                    strongSelf.removeDetections()
                }
                return Empty().eraseToAnyPublisher()
            }
            .flatMap({ codes -> AnyPublisher<Barcode, Error> in
                return strongSelf.reduceToNew(codes: codes)
            })
            .receive(on: DispatchQueue.main)
            .map({ barcode in
                strongSelf.stopSession()
                return barcode
            })
            .flatMap { code  -> AnyPublisher<Barcode, Error> in
                return strongSelf.mark(code: code)
            }
            .sink(receiveCompletion: { _ in
            }, receiveValue: { code in
                let type = BarcodeType.from(format: code.format)
                strongSelf.delegate?.didSelect(Code(value: code.displayValue!, type: type))
                strongSelf.close()
            }).store(in: &cancellables)
    }
    
    /**
     Creates a vision image based on a UIImage.
     */
    func getVisionImage(_ image: UIImage) -> AnyPublisher<VisionImage, Never>  {
        let orientation = UIUtilities.imageOrientation(
            fromDevicePosition: .back
        )
        let visionImage = VisionImage(image: image)
        visionImage.orientation = orientation
        return Just(visionImage).eraseToAnyPublisher()
    }
    
    /**
     Scans for barcodes in an vision image. Returns all found barcodes.
     */
    func scanForBarcodeIn(_ image: VisionImage) -> AnyPublisher<[Barcode], Error> {
        // Define the options for a barcode detector.
        let format = BarcodeFormat.all
        let barcodeOptions = BarcodeScannerOptions(formats: format)

        // Create a barcode scanner.
        let barcodeScanner = BarcodeScanner.barcodeScanner(options: barcodeOptions)
        var barcodes: [Barcode]
        do {
            barcodes = try barcodeScanner.results(in: image)
            return Just(barcodes).setFailureType(to: Error.self).eraseToAnyPublisher()
        } catch let error {
            print("Failed to scan barcodes with error: \(error.localizedDescription).")
            return Fail<[Barcode], Error>(error: BarcodeError.failedToScan).eraseToAnyPublisher()
        }
    }
    
    /**
     Validates if the found barcodes not empty and matches the requirements of the delegate. Returns a list of valid barcodes.
     */
    func validate(codes: [Barcode]) -> AnyPublisher<[Barcode], Error> {
        return Publishers.MergeMany(
            codes.map { barcode in
                return validate(code: barcode)
                    .setFailureType(to: Error.self)
                    .tryMap({ isValid in
                        if isValid { return barcode }
                        throw BarcodeError.invalidBarcode
                    })
                    .eraseToAnyPublisher()
                }
            ).collect()
            .eraseToAnyPublisher()
    }
    
    private func validate(code: Barcode) -> AnyPublisher<Bool, Never> {
        return Future<Bool, Never> { [weak self] promise in
            if let value = code.displayValue {
                let type = BarcodeType.from(format: code.format)
                if let mDelegate = self?.delegate {
                    mDelegate.isValid(Code(value: value, type: type)) { result in
                        promise(Result.success(result))
                    }
                } else {
                    promise(Result.success(false))
                }
            } else {
                promise(Result.success(false))
            }
        }.eraseToAnyPublisher()
    }
    
    /**
     Compare detected barcode with the currently stored codes.
     */
    func reduceToNew(codes: [Barcode]) -> AnyPublisher<Barcode, Error> {
        let lastBarcodes = detectedBarcodes
        detectedBarcodes = codes
        if lastBarcodes.first?.displayValue == codes.first?.displayValue {
            return Fail(error: BarcodeError.alreadyMarked)
                .eraseToAnyPublisher()
        } else {
            if let code = detectedBarcodes.first {
                return Just(code)
                    .setFailureType(to: Error.self)
                    .eraseToAnyPublisher()
            }
            return Fail(error: BarcodeError.nothingFound)
                .eraseToAnyPublisher()
        }
    }
    
    /**
     Displays a barcode in the result view animated.
     */
    func mark(code: Barcode) -> AnyPublisher<Barcode, Error> {
        Future<Barcode, Error> { [weak self] promise in
            guard let strongSelf = self else {
                promise(Result.failure(BarcodeError.couldNotDisplayError))
                return
            }
            strongSelf.updateResultView(barcode: code) {
                promise(Result.success(code))
            }
        }
        .eraseToAnyPublisher()
    }
    
    /**
     Removes the result view.
     */
    private func removeDetections() {
        resultView.isHidden = true
    }
}
