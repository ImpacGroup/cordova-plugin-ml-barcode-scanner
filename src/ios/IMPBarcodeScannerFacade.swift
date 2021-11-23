//
//  IMPBarcodeScannerFacade.swift
//  MLBarcodeScanner
//
//  Created by Felix Nievelstein on 18.11.21.
//

import Foundation
import Combine

enum CordovaError: Error {
    case WAITING_RESPONSE
    case INTERNAL_ERROR
}


@objc (MlBarcodeScanner) class IMPBarcodeScannerFacade: CDVPlugin, CameraViewControllerDelegate {
    
    private var infoScreen: ScannerInfo? = nil
    
    private var updateCallbackId: String?
    
    private lazy var cancellables: Set<AnyCancellable> = []
    
    private lazy var answers: [Answer] = []
    
    private lazy var codesToValidate: [Code] = []
    
    /**
     Open Scanner
     */
    @objc(openScanner:) func openScanner(command: CDVInvokedUrlCommand) {
        updateCallbackId = command.callbackId
        let title = command.arguments.count == 1 && command.arguments[0] as? String != nil ? command.arguments[0] as! String : "Scanner"
        let cameraVC = CameraViewController(nibName: "Camera", bundle: nil)
        cameraVC.modalPresentationStyle = .overFullScreen
        cameraVC.title = title
        cameraVC.delegate = self
        viewController.present(cameraVC, animated: true, completion: nil)
    }
    
    /**
     Answer if is a valid barcode
     */
    @objc(validationResult:) func validationResult(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 1, let infoJson = command.arguments[0] as? String {
            do {
                let decoder = JSONDecoder()
                if let data = infoJson.data(using: String.Encoding.utf8) {
                    let answer = try decoder.decode(ValidationAnswer.self, from: data)
                    answers.append(answer)
                }
            }
            catch {
                let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.localizedDescription)
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }
        }
    }
    
    /**
     Set properties for info screen below scanner.
     */
    @objc(setInfoScreen:) func setInfoScreen(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 1, let json = command.arguments[0] as? String {
            do {
                let decoder = JSONDecoder()
                if let data = json.data(using: String.Encoding.utf8) {
                    infoScreen = try decoder.decode(ScannerInfo.self, from: data)
                }
            }
            catch {
                let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.localizedDescription)
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }
        }
    }
    
    func didSelect(_ code: Code) {
        sendUpdateMessage(message: ScannerMessage<Code>(action: "didSelectCode", object: code), status: CDVCommandStatus_OK)
    }
    
    func setScreen() -> ScannerInfo {
        return infoScreen != nil ? infoScreen! : ScannerInfo(title: "", text: "", button: ScannerButton(title: ""))
    }
    
    func preferredManuelInput() {
        sendUpdateMessage(message: ScannerMessage<String>(action: "preferredManuelInput"), status: CDVCommandStatus_OK)
    }
    
    func setResult(_ code: Code, completion: @escaping (ScannerResult) -> ()) {
        let request = Request<Code>(action: "resultScreen", object: code)
        request.responseTo = ""
        
        sendUpdateMessage(message: request, status: CDVCommandStatus_OK)
        completion(ScannerResult(title: "Test"))
    }
    
    func isValid(_ code: Code, completion: @escaping (Bool) -> ()) {
        if !codesToValidate.contains(where: { c in
            c.value == code.value && c.type == code.type
        }) {
            codesToValidate.append(code)
            let request = Request<Code>(action: "isValid", object: code)
            request.responseTo = "validationResult"
            sendUpdateMessage(message: request, status: CDVCommandStatus_OK)
            
            answerFor(requestId: request.id)
                .catch { error -> AnyPublisher<Answer, Error> in
                    print("Error Validating: \(error)")
                    completion(false)
                    return Empty().eraseToAnyPublisher()
                }
                .sink(receiveCompletion: {[weak self] _ in
                    if let index = self?.codesToValidate.firstIndex { c in
                        c.value == code.value && c.type == code.type
                    } {
                        self?.codesToValidate.remove(at: index)
                    }
                }, receiveValue: { answer in
                    if let mAnswer = answer as? ValidationAnswer {
                        completion(mAnswer.result)
                    } else {
                        print("Invalid Answer type")
                    }
                })
                .store(in: &cancellables)
        }
    }
    
    /**
     Publisher returns a answer for a given requestId. Timeout after 3 seconds without answer.
     */
    private func answerFor(requestId: String) -> AnyPublisher<Answer, Error> {
        return Just<Void>(())
            .setFailureType(to: CordovaError.self)
            .delay(for: 0.1, scheduler: DispatchQueue.global())
            .tryMap({ [weak self] _ -> Answer in
                guard let strongSelf = self else {
                    throw CordovaError.INTERNAL_ERROR
                }
                if let answer = strongSelf.answers.first(where: { a in a.requestId == requestId }) {
                    return answer
                }
                throw CordovaError.WAITING_RESPONSE
            })
            .retry(30)
            .eraseToAnyPublisher()
    }
    
    
    func willClose() {
        sendUpdateMessage(message: ScannerMessage<String>(action: "willClose"), status: CDVCommandStatus_OK)
    }
    
    func didClose() {
        sendUpdateMessage(message: ScannerMessage<String>(action: "didClose"), status: CDVCommandStatus_OK, keep: false)
        cancellables.removeAll()
    }
    
    private func sendUpdateMessage<T: Encodable>(message: T, status: CDVCommandStatus, keep: Bool = true) {
        if let callbackId = updateCallbackId {
            do {
                let jsonData = try JSONEncoder().encode(message)
                let result = CDVPluginResult(status: status, messageAs: String(data: jsonData, encoding: .utf8))
                result?.keepCallback = NSNumber(value: keep)
                self.commandDelegate.send(result, callbackId: callbackId)
            } catch  {
                print(error)
            }
        }
    }
}

struct ValidationAnswer: Answer, Decodable {
    var requestId: String
    
    let result: Bool
}

struct ResultScreenAnswer: Answer, Decodable {
    var requestId: String
    
    var result: ScannerResult
}

protocol Answer {
    var requestId: String { get }
}

class Request<T: Encodable>: Encodable {
    let id: String
    var responseTo: String? = nil
    var object: T? = nil
    let action: String
    
    init(action: String, object: T? = nil) {
        id = UUID().uuidString
        self.object = object
        self.action = action
    }
}

class ScannerMessage<T: Encodable>: Encodable {
    var object: T? = nil
    let action: String
    
    init(action: String, object: T? = nil) {
        self.object = object
        self.action = action
    }
}

