package me.xana.snsclient.framework.endpoint

import androidx.annotation.DrawableRes
import okhttp3.Headers
import okhttp3.RequestBody

/**
 * 服务端点，每个端点子类代表一种客户端协议
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */
abstract class EndPoint(

    val name: String,

    @DrawableRes
    val iconRes: Int
) {

    abstract fun processAuthHeader(headers: Headers, request: RequestBody)

    abstract fun processRequestHeader(headers: Headers, request: RequestBody)
}