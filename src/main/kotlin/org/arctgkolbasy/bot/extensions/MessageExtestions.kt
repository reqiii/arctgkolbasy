package org.arctgkolbasy.bot.extensions

fun String.escapeForMarkdownV2(): String {
    val builder = StringBuilder()
    for (c in toCharArray()) {
        if (!c.isLetterOrDigit()) {
            builder.append("\\")
        }
        builder.append(c)
    }
    return builder.toString()
}
