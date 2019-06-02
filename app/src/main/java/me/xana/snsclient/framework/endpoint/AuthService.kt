package me.xana.snsclient.framework.endpoint

import android.net.Uri
import okhttp3.ResponseBody

/**
 * 授权服务基类
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */
abstract class AuthService<E : EndPoint, T: AuthToken>(
    var authCallback: AuthCallback<E, T>
) {

    abstract fun loginWithOAuth(endPoint: E): Uri

    abstract fun loginWithXAuth(endPoint: E, username: String, password: String)

    interface AuthCallback<E, T> {

        fun getAuthToken(endPoint: E, response: ResponseBody): T?
    }
}