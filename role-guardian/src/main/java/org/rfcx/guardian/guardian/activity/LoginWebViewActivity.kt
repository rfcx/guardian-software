package org.rfcx.guardian.guardian.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.auth0.android.result.Credentials
import kotlinx.android.synthetic.main.activity_login_webview.*
import org.json.JSONObject
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.manager.CredentialKeeper
import org.rfcx.guardian.guardian.manager.CredentialVerifier
import org.rfcx.guardian.utility.network.TLSSocketFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection


class LoginWebViewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_webview)

        val uri = intent.data
        if (uri != null) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                preparePost(code)
            }
        } else {
            Log.d("LoginWebViewActivity", baseUrl)
            val webpage = Uri.parse(baseUrl)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
            }
        }
        codeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count < 0) {
                    sendButton.isEnabled = false
                    sendButton.alpha = 0.5f
                } else {
                    sendButton.isEnabled = true
                    sendButton.alpha = 1.0f
                }
            }
        })

        sendButton.setOnClickListener {
            preparePost(null)
        }

    }

    private fun preparePost(code: String?) {
        val handler = Handler()
        loginProgressBar.visibility = View.VISIBLE
        loginLayout.visibility = View.INVISIBLE
        //dismiss keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeEditText.windowToken, 0)
        val runnable = Runnable {
            val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val postUrl = "https://auth.rfcx.org/oauth/token"
            val body = hashMapOf<String, String>(
                "grant_type" to "authorization_code",
                "client_id" to clientId,
                "code" to (code ?: codeEditText.text.toString()),
                "redirect_uri" to redirectUrl
            )

            val postResponse = post(postUrl, body)
            Log.d("LoginWebViewActivity", postResponse)
            if (postResponse.isNotEmpty()) {
                val response = JSONObject(postResponse)
                val idToken = response.getString("id_token")
                val accessToken = response.getString("access_token")
                val credentials = Credentials(idToken, accessToken, null, null, 86400000)
                val result = CredentialVerifier(this).verify(credentials)
                when (result) {
                    is Err -> {
                        Log.d("LoginWebViewActivity", "login error")
                        loginProgressBar.visibility = View.INVISIBLE
                        loginLayout.visibility = View.VISIBLE
                    }
                    is Ok -> {
                        CredentialKeeper(this).save(result.value)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                Log.d("LoginWebViewActivity", credentials.idToken)
            } else {
                Log.d("LoginWebViewActivity", "post failed")
                Toast.makeText(this, "code is incorrect.", Toast.LENGTH_LONG).show()
                loginProgressBar.visibility = View.INVISIBLE
                loginLayout.visibility = View.VISIBLE
            }
            codeEditText.text = null
        }
        handler.post(runnable)
    }

    private fun post(url: String, params: HashMap<String, String>): String {
        var response = ""
        try {
            val url = URL(url)
            val conn = url.openConnection() as HttpsURLConnection
            conn.sslSocketFactory = TLSSocketFactory()
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true


            val os = conn.outputStream
            val writer = BufferedWriter(
                OutputStreamWriter(os, "UTF-8")
            )
            writer.write(getPostDataString(params))

            writer.flush()
            writer.close()
            os.close()
            val responseCode = conn.responseCode

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String? = ""
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                while ((line) != null) {
                    line = br.readLine()
                    response += line
                }
            } else {
                response = ""

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return response
    }

    private fun getPostDataString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for (entry in params.entries) {
            if (first)
                first = false
            else
                result.append("&")

            result.append(URLEncoder.encode(entry.key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(entry.value, "UTF-8"))
        }

        return result.toString()
    }


    companion object {
        private const val redirectUrl = "rfcx://login"
        private const val audience = "https://rfcx.org"
        private const val scope = "openid%20email%20profile"
        private const val clientId = "CdlIIeJDapQxW29kn93wDw26fTTNyDkp"
        const val baseUrl =
            "https://auth.rfcx.org/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUrl}&audience=${audience}&scope=${scope}"
    }
}
