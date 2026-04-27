package com.android.messaging.domain.conversation.usecase.telephony

import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.android.messaging.util.LogUtil
import javax.inject.Inject

internal fun interface IsEmergencyPhoneNumber {
    operator fun invoke(phoneNumber: String): Boolean
}

internal class IsEmergencyPhoneNumberImpl @Inject constructor(
    private val telephonyManager: TelephonyManager,
) : IsEmergencyPhoneNumber {

    @Suppress("DEPRECATION")
    override operator fun invoke(phoneNumber: String): Boolean {
        val normalizedPhoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber)

        return try {
            telephonyManager.isEmergencyNumber(normalizedPhoneNumber)
        } catch (exception: IllegalStateException) {
            LogUtil.w(LOG_TAG, "Unable to check emergency phone number", exception)
            PhoneNumberUtils.isEmergencyNumber(normalizedPhoneNumber)
        } catch (exception: UnsupportedOperationException) {
            LogUtil.w(LOG_TAG, "Unable to check emergency phone number", exception)
            PhoneNumberUtils.isEmergencyNumber(normalizedPhoneNumber)
        }
    }

    private companion object {
        private const val LOG_TAG = "IsEmergencyPhoneNumber"
    }
}
