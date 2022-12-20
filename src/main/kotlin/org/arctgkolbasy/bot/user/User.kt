package org.arctgkolbasy.bot.user

data class User(
    val id: Long,
    val telegramId: Long,
    val isBot: Boolean,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val roles: Set<UserRoles>,
    val sessionKey: String?,
    val session: String?,
)
