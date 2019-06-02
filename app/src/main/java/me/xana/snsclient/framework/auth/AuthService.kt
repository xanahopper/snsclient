package me.xana.snsclient.framework.auth

import android.net.Uri
import me.xana.snsclient.framework.endpoint.EndPoint
import okhttp3.ResponseBody

/**
 * 授权服务基类
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */
interface AuthService<E : EndPoint, T: AuthToken>{

    fun loginWithOAuth(endPoint: E): Uri

    fun loginWithXAuth(endPoint: E, username: String, password: String)

    fun getAuthToken(endPoint: E, response: ResponseBody): T?

    val oauthCallback: String
}