package org.arctgkolbasy.bot.user

data class User(
    val id: Long,
    val isBot: Boolean,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val roles: List<UserRoles> = listOf(UserRoles.USER),
)
