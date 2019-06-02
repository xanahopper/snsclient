package me.xana.snsclient.framework.auth

import org.joda.time.DateTime

/**
 * @author Xana Hopper(xanarust@gmail.com)
 */
abstract class AuthToken(
    val endPointName: String,
    val expiredTime: Long
) {

    val isExpired: Boolean
        get() = DateTime.now().isAfter(expiredTime)
}