package org.arctgkolbasy.bot.user

data class User (
    val id: Long,
    val telegramId: Long,
    var isBot: Boolean,
    var firstName: String,
    var lastName: String? = null,
    var username: String? = null,
    var roles: Set<UserRoles>,
)
