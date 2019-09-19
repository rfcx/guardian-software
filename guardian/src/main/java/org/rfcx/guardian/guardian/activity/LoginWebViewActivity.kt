package org.rfcx.guardian.guardian.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.auth0.android.result.Credentials
import kotlinx.android.synthetic.main.activity_login_webview.*
import org.json.JSONObject
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.manager.CredentialKeeper
import org.rfcx.guardian.guardian.manager.CredentialVerifier
import org.rfcx.guardian.guardian.utils.TLSSocketFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection


class LoginWebViewActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_webview)

        setSupportActionBar(toolbar)
        toolBarInit()

        runOnUiThread {
            Log.d("LoginWebViewActivity", baseUrl)
            val webpage = Uri.parse(baseUrl)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }

        val handler = Handler()

        sendButton.setOnClickListener {
            val runnable = Runnable {
                val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
                StrictMode.setThreadPolicy(policy)
                val postUrl = "https://auth.rfcx.org/oauth/token"
                val body = hashMapOf<String, String>(
                    "grant_type" to "authorization_code",
                    "client_id" to clientId,
                    "code" to codeEditText.text.toString(),
                    "redirect_uri" to redirectUrl
                )

                val postResponse = post(postUrl, body)
                Log.d("LoginWebViewActivity", postResponse)
                if(postResponse.isNotEmpty()){
                    val response = JSONObject(postResponse)
                    val idToken = response.getString("id_token")
                    val accessToken = response.getString("access_token")
                    val credentials = Credentials(idToken, accessToken,null, null, null)
                    val result = CredentialVerifier(this).verify(credentials)
                    when(result){
                        is Err -> {
                            Log.d("LoginWebViewActivity", "login error")
                        }
                        is Ok -> {
                            CredentialKeeper(this).save(result.value)
                            finish()
                        }
                    }
                    Log.d("LoginWebViewActivity", credentials.idToken)
                }else{
                    Log.d("LoginWebViewActivity", "post failed")
                }
                codeEditText.text = null
            }
            handler.post(runnable)
        }

    }

    private fun toolBarInit() {
        val toolbar = supportActionBar
        toolbar?.title = "Login"
    }
    private fun post(url: String, params: HashMap<String, String>): String{
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

    private fun getPostDataString(params: HashMap<String, String>): String{
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


    companion object{
        private const val redirectUrl = "https://rfcx-app.s3.eu-west-1.amazonaws.com/login/cli.html"
        private const val audience = "https://rfcx.org"
        private const val scope = "openid%20profile"
        private const val clientId = "CdlIIeJDapQxW29kn93wDw26fTTNyDkp"
        const val baseUrl = "https://auth.rfcx.org/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUrl}&audience=${audience}&scope=${scope}"
    }
}