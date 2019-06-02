package me.xana.snsclient.framework.account

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.GsonBuilder
import me.xana.snsclient.framework.auth.AuthToken
import me.xana.snsclient.framework.gson.authTokenTypeAdapterFactory

/**
 * 账户信息
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */
@Entity(tableName = "account")
@TypeConverters(AuthTokenConverter::class)
data class Account(
    val endPointName: String,
    val displayName: String,
    val authToken: AuthToken,
    val avatar: String?
)

class AuthTokenConverter {

    @TypeConverter
    fun toString(token: AuthToken): String {
        return gson.toJson(token)
    }

    @TypeConverter
    fun toToken(value: String): AuthToken {
        return gson.fromJson(value, AuthToken::class.java)
    }

    companion object {

        private val gson by lazy { GsonBuilder().registerTypeAdapterFactory(authTokenTypeAdapterFactory).create() }
    }
}