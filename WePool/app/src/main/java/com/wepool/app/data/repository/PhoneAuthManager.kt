package com.wepool.app.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PhoneAuthManager {
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastSentPhoneNumber: String? = null

    suspend fun sendVerificationCode(phoneNumber: String, activity: Activity): Boolean =
        suspendCancellableCoroutine { cont ->
            lastSentPhoneNumber = phoneNumber
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d("PhoneAuth", "✔️ אימות אוטומטי הושלם")
                        cont.resume(true)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("PhoneAuth", "❌ שגיאה באימות: ${e.message}", e)
                        cont.resumeWithException(e)
                    }

                    override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = vid
                        resendToken = token
                        Log.d("PhoneAuth", "📨 קוד נשלח ל-$phoneNumber")
                        cont.resume(true)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

    fun verifyCode(code: String, phoneNumber: String): PhoneAuthCredential {
        val vid = verificationId ?: throw IllegalStateException("❌ Verification ID is null")
        if (phoneNumber != lastSentPhoneNumber) {
            throw IllegalStateException("מספר הטלפון שונה מזה שנשלח אליו הקוד")
        }
        return PhoneAuthProvider.getCredential(vid, code)
    }

    fun normalizePhoneNumber(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("+972") -> trimmed
            trimmed.startsWith("0") -> "+972" + trimmed.drop(1)
            else -> "+972$trimmed" // fallback
        }
    }
}