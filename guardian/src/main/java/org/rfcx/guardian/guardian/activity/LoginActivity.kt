package org.rfcx.guardian.guardian.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.result.Credentials
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.UserAuthResponse
import org.rfcx.guardian.guardian.manager.Constant
import org.rfcx.guardian.guardian.manager.CredentialKeeper
import org.rfcx.guardian.guardian.manager.CredentialVerifier



class LoginActivity : AppCompatActivity() {

    private var isAnonymousLogin: Boolean = false

    private val auth0 by lazy {
        val auth0 = Auth0(this)
        auth0.isLoggingEnabled = true
        auth0.isOIDCConformant = true
        auth0
    }
    private val authentication by lazy {
        AuthenticationAPIClient(auth0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // enable tls1.2 for sending https request
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            try {
                ProviderInstaller.installIfNeeded(this)
            } catch (e: GooglePlayServicesRepairableException) {
                Log.d("google", "please install gg service")
                GoogleApiAvailability.getInstance()
                    .showErrorNotification(this, e.connectionStatusCode)
            } catch (e: GooglePlayServicesNotAvailableException) {
                Log.d("google", "you cannot use it")
            }
        }

        // to check if user have logined or not
        if (CredentialKeeper(this).hasValidCredentials()) {
            loginGroupView.visibility = View.INVISIBLE

            MainActivity.startActivity(this@LoginActivity)
            finish()
        } else {
            loginGroupView.visibility = View.VISIBLE
        }
        loginGroupView.visibility = View.VISIBLE

        loginButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            if (validateInput(email, password)) {
                doLogin(email, password)
            }
        }

        skipLogin.setOnClickListener {
            loginAnonymously()
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

    private fun loginAnonymously() {
        Log.i("LoginActivity", "here we go")

//        authentication
//            .login(Constant().AUTH0_ANON_USERNAME, Constant().AUTH0_ANON_PASSWORD, "Username-Password-Authentication")
//            .setAudience(getString(R.string.auth0_audience))
//            .start(object : BaseCallback<Credentials, AuthenticationException> {
//                override fun onSuccess(credentials: Credentials) {
//                    loginSuccess(credentials, "anonymous")
//                }
//
//                override fun onFailure(exception: AuthenticationException) {
//                    loginFailed(exception)
//                }
//            })

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("LOGIN_STATUS", "skip")
        startActivity(intent)

    }

    private fun doLogin(email: String, password: String) {
        loginGroupView.visibility = View.GONE
        skipLogin.visibility = View.INVISIBLE
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
                        is Err -> {
                            loginFailed(result.error)
                        }
                        is Ok -> {
                            loginSuccess(credentials, result.value)
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    exception.printStackTrace()
//                    Crashlytics.logException(exception)
                    if (exception.code == "invalid_grant") {
                        loginFailed(getString(R.string.incorrect_username_password))
                    } else {
                        loginFailed(exception.description)
                        Log.d("errorlogin", exception.description)
                    }
                }
            })
    }

    private fun loginFailed(errorMessage: String?) {
        runOnUiThread {
            loginGroupView.visibility = View.VISIBLE
            skipLogin.visibility = View.VISIBLE
            loginProgress.visibility = View.INVISIBLE

            loginButton.isEnabled = true
            loginEmailEditText.isEnabled = true
            loginPasswordEditText.isEnabled = true
            loginErrorTextView.text = errorMessage
            loginErrorTextView.visibility = View.VISIBLE
        }

    }

    private fun loginSuccess(credentials: Credentials, userAuthResponse: UserAuthResponse) {

        Log.i("LoginActivity", "Auth0: success")
        CredentialKeeper(this@LoginActivity).save(userAuthResponse)
        Log.d("userauth", userAuthResponse.idToken + " " + userAuthResponse.accessToken)
        Log.d("credentials", credentials.idToken + " " + credentials.accessToken)

        MainActivity.startActivity(this@LoginActivity)
        finish()
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }
    }

}