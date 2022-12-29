package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.user.UserRepository
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller

@Controller
class RoleRemoverCommand(
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
    override fun getCommandName(): String = REMOVE_USER_ROLE

    override fun checkUserAccess(user: User): Boolean = UserRoles.ADMIN in user.roles

    override fun sessionCheck(sessionKey: String?, session: String?): Boolean = sessionKey in steps

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) = when (user.sessionKey) {
        RemoveRoleStates.STEP_1_CHOOSE_USER.step -> stepOneChooseUser(update, bot, user)
        RemoveRoleStates.STEP_2_REMOVE_ROLE.step -> stepTwoRemoveRole(update, bot, user)
        else -> stepZero(update, bot, user)
    }

    private fun stepZero(update: Update, bot: Bot, user: User) {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = userRepository.findAll().joinToString(
                prefix = "Выбери пользователя:\n",
                separator = "\n",
                transform = { u ->
                    "/${u.username.toString()} ${
                        u.roles.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { role -> role.roleName.name }
                    }"
                }
            )
        )
        user.updateSession(RemoveRoleStates.STEP_1_CHOOSE_USER.step)
    }

    private fun stepOneChooseUser(update: Update, bot: Bot, user: User) {
        val name = update.message().replaceFirst("/", "")
        bot.sendMessage(
            update.chatIdUnsafe(),
            userRepository.findByUsername(name)?.roles?.joinToString(
                prefix = "Текущие роли пользователя ${name}:\n",
                separator = "\n",
                transform = { "/${it.roleName.name}" }
            ) ?: return user.clearSession()
        )
        user.updateSession(
            sessionKey = RemoveRoleStates.STEP_2_REMOVE_ROLE.step,
            session = objectMapper.writeValueAsString(AddRoleSession(username = name))
        )
    }

    private fun stepTwoRemoveRole(update: Update, bot: Bot, user: User) {
        val role = when (update.message().replaceFirst("/", "")) {
            UserRoles.ADMIN.name -> UserRoles.ADMIN
            UserRoles.USER.name -> UserRoles.USER
            UserRoles.GUEST.name -> UserRoles.GUEST
            else -> throw IllegalArgumentException("Недопустимая роль!")
        }
        val removeRoleSession = objectMapper.readValue<RoleSetterCommand.AddRoleSession>(user.session!!)
        userService.deleteUserRoles(removeRoleSession.username, role)
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Роль $role пользователя ${removeRoleSession.username} удалена."
        )
        user.clearSession()
    }

    enum class RemoveRoleStates(
        val step: String,
    ) {
        STEP_1_CHOOSE_USER("${REMOVE_USER_ROLE}1"),
        STEP_2_REMOVE_ROLE("${REMOVE_USER_ROLE}2"),
    }

    companion object {
        val steps = RemoveRoleStates.values().mapTo(mutableSetOf()) { it.step }
        const val REMOVE_USER_ROLE = "remove_user_role"
    }

    data class AddRoleSession(
        val username: String,
    )
}