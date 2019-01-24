package com.siddhantkushwaha.raven.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.siddhantkushwaha.raven.R
import com.siddhantkushwaha.raven.common.utility.ActivityInfo
import com.siddhantkushwaha.raven.common.utility.Alerts
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    companion object {
        data class IntentData(val dummy: String)
        fun openActivity(activity: Activity, finish: Boolean) {

            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intent)
            if (finish)
                activity.finish()
        }
    }

    private val tag = LoginActivity::class.java.toString()

    private val activityStateEnterPhone = "ENTER_PHONE"
    private val activityStateSendingCode = "SENDING_CODE"
    private val activityStateCodeSent = "CODE_SENT"
    private val activityStateVerifyingCode = "VERIFYING_CODE"
    private val activityStateCodeVerificationFailed = "CODE_VERIFICATION_FAILED"
    private val activityStateCodeVerificationSuccess = "CODE_VERIFICATION_SUCCESS"
    private val activityStateSigningIn = "SIGNING_IN"
    private val activityStateSignInFailed = "SIGN_IN_FAILED"
    private val activityStateSignInSuccess = "SIGN_IN_SUCCESS"

    private var activityState: EditText? = null
    private var activityStateFlag: Int = 0

    private var phoneVerificationId: String? = null
    private var phoneOTPResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneVerificationCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Fabric.with(this@LoginActivity, Crashlytics())
        Crashlytics.setUserIdentifier("NONE")
        Crashlytics.setUserName("NONE")

        setContentView(R.layout.activity_login)

        ccp.registerCarrierNumberEditText(phone)

        activityState = EditText(this@LoginActivity)
        activityState?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                when (s.toString()) {
                    activityStateEnterPhone -> {
                        setVisibility(intArrayOf(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE))
                    }
                    activityStateSendingCode -> {
                        showAlert("Sending OTP.", activityStateFlag)
                        progressBar.visibility = View.VISIBLE
                        setVisibility(intArrayOf(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE))

                        if (activityStateFlag == 1)
                            resendOTP()
                    }
                    activityStateCodeSent -> {
                        showAlert("OTP sent.", activityStateFlag)
                        progressBar.visibility = View.GONE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                    activityStateVerifyingCode -> {
                        showAlert("Verifying OTP", activityStateFlag)
                        progressBar.visibility = View.VISIBLE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))

                        if (activityStateFlag == 1)
                            verifyOtp()
                    }
                    activityStateCodeVerificationFailed -> {
                        showAlert("Verification failed.", activityStateFlag)
                        progressBar.visibility = View.GONE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                    activityStateCodeVerificationSuccess -> {
                        showAlert("Verification successful.", activityStateFlag)
                        progressBar.visibility = View.GONE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                    activityStateSigningIn -> {
                        showAlert("Signing in.", activityStateFlag)
                        progressBar.visibility = View.VISIBLE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                    activityStateSignInFailed -> {
                        showAlert("Sign in failed.", activityStateFlag)
                        progressBar.visibility = View.GONE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                    activityStateSignInSuccess -> {
                        showAlert("Sign in successful.", activityStateFlag)
                        progressBar.visibility = View.GONE
                        setVisibility(intArrayOf(View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE))
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        submit.setOnClickListener {
            if (ccp.isValidFullNumber) {
                sendOTP()
                updateActivityState(activityStateSendingCode, 0)
            } else {
                showAlert("Not a valid number in ${ccp.selectedCountryName}.", 0)
            }
        }
        resend.setOnClickListener {
            if (ccp.isValidFullNumber) {
                resendOTP()
                updateActivityState(activityStateSendingCode, 0)
            }
        }
        verify.setOnClickListener {
            verifyOtp()
            updateActivityState(activityStateVerifyingCode, 0)
        }
        wrongPhone.setOnClickListener {
            updateActivityState(activityStateEnterPhone, 0)
        }

        phoneVerificationCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

                updateActivityState(activityStateCodeVerificationSuccess, 0)
                if (FirebaseAuth.getInstance().currentUser == null)
                    signIn(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {

                Log.e(tag, e.toString())
                updateActivityState(activityStateCodeVerificationFailed, 0)
            }

            override fun onCodeSent(s: String?, forceResendingToken: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(s, forceResendingToken)

                updateActivityState(activityStateCodeSent, 0)

                phoneVerificationId = s
                phoneOTPResendingToken = forceResendingToken
            }
        }
    }

    override fun onStart() {
        super.onStart()

        ActivityInfo.setActivityInfo(this::class.java.toString(), intent.extras)

        if (FirebaseAuth.getInstance().currentUser != null)
            HomeActivity.openActivity(this@LoginActivity, true, HomeActivity.Companion.IntentData(""))
    }

    override fun onPause() {
        super.onPause()

        ActivityInfo.setActivityInfo(null, null)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString("LAYOUT_STATE", activityState?.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        updateActivityState(savedInstanceState?.getString("LAYOUT_STATE"), 1)
    }

    private fun updateActivityState(state: String?, flag: Int) {

        activityStateFlag = flag
        activityState?.setText(state ?: activityStateEnterPhone)
    }

    private fun showAlert(message: String, flag: Int) {
        if (flag == 0)
            Alerts.showSnackbar(scrollView, message, 2000)
    }

    private fun setVisibility(visibility: IntArray) {
        val views: Array<View> = arrayOf(phoneLinearLayout, otp, resend, verify, submit, wrongPhone)
        views.forEachIndexed { index, view ->
            view.visibility = visibility[index]
        }
    }

    private fun sendOTP() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(ccp.fullNumberWithPlus, 60, TimeUnit.SECONDS, this@LoginActivity, phoneVerificationCallback!!)
    }

    private fun resendOTP() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(ccp.fullNumberWithPlus, 60, TimeUnit.SECONDS, this@LoginActivity, phoneVerificationCallback!!, phoneOTPResendingToken)
    }

    private fun verifyOtp() {
        if (phoneVerificationId != null) {
            signIn(PhoneAuthProvider.getCredential(phoneVerificationId.toString(), otp.text.toString()))
        } else {
            updateActivityState(activityStateCodeVerificationFailed, 0)
        }
    }

    private fun signIn(phoneAuthCredential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener { t ->

            if (t.isSuccessful) {

                updateActivityState(activityStateSignInSuccess, 0)
                HomeActivity.openActivity(this@LoginActivity, true, HomeActivity.Companion.IntentData(""))
            } else {
                Log.e(tag, t.exception.toString())
                updateActivityState(activityStateSignInFailed, 0)
            }
        }
    }
}