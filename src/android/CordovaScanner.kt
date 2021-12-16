package de.impacgroup.mlbarcodescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import de.impacgroup.mlbarcodescanner.module.CameraActivity
import de.impacgroup.mlbarcodescanner.module.Code
import de.impacgroup.mlbarcodescanner.module.ScannerResult
import kotlinx.coroutines.delay
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class CordovaScanner : CameraActivity() {

    var answers: MutableList<Answer> = ArrayList()

    companion object {
        const val CORDOVA_SCANNER_STATE_ACTION = "cordova-scanner-state-action"
        const val CORDOVA_SCANNER_STATE_ACTION_KEY = "scannerState"
        const val CORDOVA_SCANNER_PARAM_ACTION_KEY = "scannerActionParameter"

        const val CORDOVA_SCANNER_VALIDATION_INTENT_KEY = "CORDOVA_SCANNER_VALIDATION_INTENT_KEY"
        const val CORDOVA_SCANNER_VALIDATION_ANSWER_INTENT_KEY = "CORDOVA_SCANNER_VALIDATION_ANSWER_INTENT_KEY"
        const val CORDOVA_SCANNER_ANSWER_EXTRA_KEY = "CORDOVA_SCANNER_ANSWER_EXTRA_KEY"

        const val CORDOVA_SCANNER_RESULT_INTENT_KEY = "CORDOVA_SCANNER_RESULT_INTENT_KEY"
        const val CORDOVA_SCANNER_RESULT_ANSWER_INTENT_KEY = "CORDOVA_SCANNER_RESULT_ANSWER_INTENT_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            resultReceiver, IntentFilter(
                CORDOVA_SCANNER_RESULT_ANSWER_INTENT_KEY
            )
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            validationReceiver, IntentFilter(
                CORDOVA_SCANNER_VALIDATION_ANSWER_INTENT_KEY
            )
        )
    }

    private val resultReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            answerFor(intent, ResultScreenAnswer::class.java)?.let {
                answers.add(it)
            }
        }
    }

    private val validationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            answerFor(intent, ValidationAnswer::class.java)?.let {
                answers.add(it)
            }
        }
    }

    private fun <T> answerFor(intent: Intent?, type: Class<T>): T? {
        intent?.let { mIntent ->
            if (mIntent.hasExtra(CORDOVA_SCANNER_ANSWER_EXTRA_KEY)) {
                try {
                    val extra = mIntent.getStringExtra(CORDOVA_SCANNER_ANSWER_EXTRA_KEY)
                    return Gson().fromJson(extra, type)
                } catch (e: Exception) {
                    Log.e("CordovaScanner", "answerFor: ", e)
                }
            }
        }
        return null
    }

    override suspend fun isValid(code: Code): Boolean {
        val request = Request("", code, ScannerAction.IS_VALID.key)
        val jsonRequest = Gson().toJson(request)
        send(ScannerAction.IS_VALID, jsonRequest, CORDOVA_SCANNER_VALIDATION_INTENT_KEY)
        try {
            val answer =  retryIO {
                getAnswer(request.id)
            }
            if (answer is ValidationAnswer) {
                return answer.result
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    @Throws
    private fun getAnswer(requestId: String): Answer  {
        return answers.first {
            it.requestId == requestId
        }
    }

    override suspend fun resultFor(code: Code): ScannerResult {
        val request = Request("setResultScreen", code, ScannerAction.RESULT_SCREEN.key)
        val jsonRequest = Gson().toJson(request)
        send(ScannerAction.RESULT_SCREEN, jsonRequest, CORDOVA_SCANNER_RESULT_INTENT_KEY)
        try {
            val answer =  retryIO {
                getAnswer(request.id)
            }
            if (answer is ResultScreenAnswer) {
                return answer.result
            }
            return ScannerResult("", null, "#999999")
        } catch (e: Exception) {
            return ScannerResult("", null, "#999999")
        }

    }

    override fun didSelect(code: Code) {
        val jsonCode = Gson().toJson(code)
        send(ScannerAction.DID_SELECT_CODE, jsonCode)
    }

    override fun onDestroy() {
        send(ScannerAction.DID_CLOSE, null)
        if (isFinishing) {
            val broadcastManager = LocalBroadcastManager.getInstance(this)
            broadcastManager.unregisterReceiver(resultReceiver)
            broadcastManager.unregisterReceiver(validationReceiver)
        }
        super.onDestroy()
    }

    override fun onPause() {
        send(ScannerAction.WILL_CLOSE, null)
        super.onPause()
    }

    private fun send(action: ScannerAction, param: String?) {
        send(action, param, CORDOVA_SCANNER_PARAM_ACTION_KEY)
    }

    private fun send(action: ScannerAction, param: String?, paramKey: String) {
        val intent = Intent(CORDOVA_SCANNER_STATE_ACTION)
        intent.putExtra(CORDOVA_SCANNER_STATE_ACTION_KEY, action.key)
        if (param != null) {
            intent.putExtra(paramKey, param)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private suspend fun <T> retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100,
        maxDelay: Long = 3000,
        factor: Double = 2.0,
        block: suspend () -> T): T
    {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: IOException) {
                // you can log an error here and/or make a more finer-grained
                // analysis of the cause to see if retry is needed
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }
}

data class Request(
    val responseTo: String?,
    val code: Code,
    val action: String
) {
    val id: String = UUID.randomUUID().toString()
}

data class ValidationAnswer(
    override val requestId: String,
    val result: Boolean
): Answer

data class ResultScreenAnswer(
    override val requestId: String,
    val result: ScannerResult
): Answer

interface Answer {
    val requestId: String
}