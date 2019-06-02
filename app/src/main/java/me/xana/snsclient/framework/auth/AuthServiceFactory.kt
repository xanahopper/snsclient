package me.xana.snsclient.framework.auth

import me.xana.snsclient.framework.endpoint.EndPoint

/**
 * 授权服务生成工程
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */
class AuthServiceFactory {

    fun createAuthService(name: String): AuthService<out EndPoint, out AuthToken>? {
        return null
    }
}