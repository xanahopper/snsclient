package me.xana.snsclient.framework.gson

import me.xana.snsclient.framework.auth.AuthToken

/**
 * 需要进行多态处理的 gson AdapterFactory
 *
 * @author Xana Hopper(xanarust@gmail.com)
 */

val authTokenTypeAdapterFactory = RuntimeTypeAdapterFactory.of(AuthToken::class.java, "endPointName")
