package com.hallysam.prefixcallblocker

import android.telecom.Call
import android.telecom.CallScreeningService as AndroidCallScreeningService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class CallScreeningService : AndroidCallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val repository = PrefixRepository(applicationContext)

        val result = runBlocking {
            withTimeoutOrNull(2500L) {
                withContext(Dispatchers.IO) {
                    val enabled = repository.isBlockingEnabled()
                    if (!enabled) {
                        ScreenResult(false, null)
                    } else {
                        val handle = callDetails.handle
                        val number = handle?.schemeSpecificPart.orEmpty()
                        if (number.isBlank()) {
                            ScreenResult(false, null)
                        } else {
                            val matchedPrefix = PhoneNumberUtils.matchesPrefix(
                                number,
                                repository.getPrefixes()
                            )
                            if (matchedPrefix == null) {
                                ScreenResult(false, null)
                            } else {
                                repository.addBlockedCall(number, matchedPrefix)
                                ScreenResult(true, matchedPrefix)
                            }
                        }
                    }
                }
            }
        }

        // Always answer within Android's screening deadline. If storage is slow or fails,
        // fail open rather than accidentally blocking a call.
        val shouldBlock = result?.blocked == true
        val response = AndroidCallScreeningService.CallResponse.Builder()
            .setDisallowCall(shouldBlock)
            .setRejectCall(shouldBlock)
            .setSkipNotification(shouldBlock)
            .build()

        respondToCall(callDetails, response)
    }

    private data class ScreenResult(
        val blocked: Boolean,
        val matchedPrefix: String?
    )
}
