package org.arctgkolbasy.bot.user

class Session(
    val sessionKey: String? = null,
    val session: String? = null,
)

val emptySession = Session(null, null)