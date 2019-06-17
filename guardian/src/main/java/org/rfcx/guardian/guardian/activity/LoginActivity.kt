package org.rfcx.guardian.guardian.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.UserAuthResponse
import org.rfcx.guardian.guardian.manager.CredentialKeeper
import org.rfcx.guardian.guardian.manager.CredentialVerifier
import org.rfcx.guardian.guardian.manager.PreferenceManager

class LoginActivity: AppCompatActivity() {

    private var isAnonymousLogin: Boolean = false

    private val auth0 by lazy {
        val auth0 = Auth0(this)
        //auth0.isLoggingEnabled = true
        auth0.isOIDCConformant = true
        auth0
    }
    private val authentication by lazy {
        AuthenticationAPIClient(auth0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (CredentialKeeper(this).hasValidCredentials()) {
            loginGroupView.visibility = View.INVISIBLE

            MainActivity.startActivity(this@LoginActivity)
            finish()
        } else {
            loginGroupView.visibility = View.VISIBLE
        }

        loginButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            if (validateInput(email, password)) {
                doLogin(email, password)
            }
        }
    }

    private fun validateInput(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty()) {
            loginEmailEditText.error = getString(R.string.pls_fill_email)
            return false
        } else if (password.isNullOrEmpty()) {
            loginPasswordEditText.error = getString(R.string.pls_fill_password)
            return false
        }
        return true
    }

    private fun doLogin(email: String, password: String) {
        loginGroupView.visibility = View.GONE
        loginProgress.visibility = View.VISIBLE
        loginErrorTextView.visibility = View.INVISIBLE
//		loginButton.isEnabled = false
//		loginEmailEditText.isEnabled = false
//		loginPasswordEditText.isEnabled = false

        authentication
            .login(email, password, "Username-Password-Authentication")
            .setScope(getString(R.string.auth0_scopes))
            .setAudience(getString(R.string.auth0_audience))
            .start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials) {
                    val result = CredentialVerifier(this@LoginActivity).verify(credentials)
                    when (result) {
                        is Err -> { loginFailed(result.error) }
                        is Ok -> { loginSuccess(result.value) }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    exception.printStackTrace()
//                    Crashlytics.logException(exception)
                    if (exception.code == "invalid_grant") {
                        loginFailed(getString(R.string.incorrect_username_password))
                    }
                    else {
                        loginFailed(exception.description)
                        Log.d("errorlogin", exception.description)
                    }
                }
            })
    }

    private fun loginFailed(errorMessage: String?) {
        runOnUiThread {
            loginGroupView.visibility = View.VISIBLE
            loginProgress.visibility = View.INVISIBLE

            loginButton.isEnabled = true
            loginEmailEditText.isEnabled = true
            loginPasswordEditText.isEnabled = true
            loginErrorTextView.text = errorMessage
            loginErrorTextView.visibility = View.VISIBLE
        }

    }

    private fun loginSuccess(userAuthResponse: UserAuthResponse) {

        CredentialKeeper(this@LoginActivity).save(userAuthResponse)

        MainActivity.startActivity(this@LoginActivity)
        finish()

//        UserTouchApi().send(this, object : UserTouchApi.UserTouchCallback {
//            override fun onSuccess() {
//                runOnUiThread{
//                    loginProgress.visibility = View.INVISIBLE
//                }
//
//                if (userAuthResponse.isRanger) {
//                    MainActivity.startActivity(this@LoginActivity)
//                }
//                else {
////                    this@LoginActivity.startActivity(Intent(this@LoginActivity, InvitationActivity::class.java))
//                }
//                finish()
//            }
//
//            override fun onFailed(t: Throwable?, message: String?) {
//                runOnUiThread{
//                    loginProgress.visibility = View.INVISIBLE
//                    loginGroupView.visibility = View.VISIBLE
//                }
//                Crashlytics.logException(t)
//                loginFailed(message ?: t?.localizedMessage)
//            }
//        })
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }
    }

}