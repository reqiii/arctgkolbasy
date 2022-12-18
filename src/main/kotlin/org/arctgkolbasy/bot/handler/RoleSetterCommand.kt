package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller

@Controller
class RoleSetterCommand(
    val userRepository: UserRepository,
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    val objectMapper: ObjectMapper,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isStateless = false,
) {
    override fun getCommandName(): String = ADD_USER_ROLE

    override fun checkUserAccess(user: User): Boolean = UserRoles.ADMIN in user.roles

    override fun sessionCheck(sessionKey: String?, session: String?): Boolean = sessionKey in steps

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) = when (user.sessionKey) {
        AddRoleStates.STEP_1_CHOOSE_USER.step -> stepOneChooseUser(update, bot, user)
        AddRoleStates.STEP_2_CHOOSE_ROLE.step -> stepTwoRoleSet(update, bot, user)
        else -> stepZero(update, bot, user)
    }

    private fun stepZero(update: Update, bot: Bot, user: User) {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = userRepository.findAll().joinToString(
                prefix = "Выбери пользователя:\n",
                separator = "\n",
                transform = { "/${it.username.toString()}" }
            )
        )
        user.updateSession(
            AddRoleStates.STEP_1_CHOOSE_USER.step,
            objectMapper.writeValueAsString(null)
        )
    }

    private fun stepOneChooseUser(update: Update, bot: Bot, user: User) {
        val name = update.message().replaceFirst("/", "")
        bot.sendMessage(
            update.chatIdUnsafe(),
            enumValues<UserRoles>().joinToString(
                prefix = "Выбран пользователь ${name}, какую роль добавить?\n",
                separator = "\n",
                transform = { "/${it.name}" }
            )
        )
        user.updateSession(
            AddRoleStates.STEP_2_CHOOSE_ROLE.step,
            objectMapper.writeValueAsString(AddRoleSession(username = name))
        )
    }

    private fun stepTwoRoleSet(update: Update, bot: Bot, user: User) {
        val role = when (update.message().replaceFirst("/", "")) {
            UserRoles.ADMIN.name -> UserRoles.ADMIN
            UserRoles.USER.name -> UserRoles.USER
            UserRoles.GUEST.name -> UserRoles.GUEST
            else -> throw IllegalArgumentException("Недопустимая роль!")
        }
        val addRoleSession = objectMapper.readValue<AddRoleSession>(user.session!!)
        userService.addUserRoles(addRoleSession.username, role)
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Роль $role для пользователя ${addRoleSession.username} добавлена"
        )
        user.clearSession()
    }

    enum class AddRoleStates(
        val step: String,
    ) {
        STEP_1_CHOOSE_USER("${ADD_USER_ROLE}1"),
        STEP_2_CHOOSE_ROLE("${ADD_USER_ROLE}2"),
    }

    companion object {
        val steps = AddRoleStates.values().mapTo(mutableSetOf()) { it.step }
        const val ADD_USER_ROLE = "add_user_role"
    }

    data class AddRoleSession(
        val username: String,
    )
}
