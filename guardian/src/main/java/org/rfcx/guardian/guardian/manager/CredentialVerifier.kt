package org.rfcx.guardian.guardian.manager

import android.content.Context
import com.auth0.android.result.Credentials
import io.jsonwebtoken.Jwts
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.Result
import org.rfcx.guardian.guardian.entity.UserAuthResponse

/**
 * Takes an Auth0 credential object and returns the parsed response
 */

class CredentialVerifier(val context: Context) {

    fun verify(credentials: Credentials): Result<UserAuthResponse, String> {
        val token = credentials.idToken
        if (token == null) {
            return Err(getString(R.string.an_error_occurred))
        }

        // Parsing JWT Token
        val metaDataKey = getString(R.string.auth0_metadata_key)
        val withoutSignature = token.substring(0, token.lastIndexOf('.') + 1)
        try {
            val untrusted = Jwts.parser().parseClaimsJwt(withoutSignature)
            if (untrusted.body[metaDataKey] == null) {
                return Err(getString(R.string.an_error_occurred))
            }

            val metadata = untrusted.body[metaDataKey]
            if (metadata == null || !(metadata is HashMap<*, *>)) {
                return Err(getString(R.string.an_error_occurred))
            }

            val guid: String? = metadata["guid"] as String?
            val email: String? = untrusted.body["email"] as String?
            val nickname: String? = untrusted.body["nickname"] as String?
            val defaultSite: String? = metadata["defaultSite"] as String?

            var accessibleSites: Set<String> = setOf()
            val accessibleSitesRaw = metadata["accessibleSites"]
            if (accessibleSitesRaw != null && accessibleSitesRaw is ArrayList<*> && accessibleSitesRaw.size > 0 && accessibleSitesRaw[0] is String) {
                @Suppress("UNCHECKED_CAST")
                accessibleSites = HashSet(accessibleSitesRaw as ArrayList<String>) // TODO: is there a better way to do this @anuphap @jingjoeh
            }

            var roles: Set<String> = setOf()
            val authorization = metadata["authorization"]
            if (authorization != null && authorization is HashMap<*, *>) {
                val rolesRaw = authorization["roles"]
                if (rolesRaw != null && rolesRaw is ArrayList<*> && rolesRaw.size > 0 && rolesRaw[0] is String) {
                    @Suppress("UNCHECKED_CAST")
                    roles = HashSet(rolesRaw as ArrayList<String>)
                }
            }

            when {
                guid.isNullOrEmpty() -> {
                    return Err(getString(R.string.an_error_occurred))
                }
                !roles.contains("guardianCreator") -> {
                    return Err("You don't have guardianCreator permission")
                }
                else -> {
                    val expireAt = credentials.expiresIn!! + System.currentTimeMillis()
                    return Ok(UserAuthResponse(guid, email, nickname, token, credentials.accessToken, credentials.refreshToken, roles, accessibleSites, defaultSite, expireAt))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Err(getString(R.string.an_error_occurred))
    }

    private fun getString(resId: Int): String = context.getString(resId)

}
