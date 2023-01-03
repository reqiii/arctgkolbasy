package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import jakarta.annotation.PostConstruct
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.user.UserRepository
import org.arctgkolbasy.user.UserService
import org.springframework.stereotype.Controller

@Controller
class RoleSetterCommand(
    val userRepository: UserRepository,
    val userService: UserService,
    val objectMapper: ObjectMapper,
) : StateMachine() {
    override fun getCommandName(): String = ADD_USER_ROLE

    override fun checkUserAccess(user: User): Boolean = UserRoles.ADMIN in user.roles

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
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
        return Session(AddRoleStates.STEP_1_CHOOSE_USER.step)
    }

    @PostConstruct
    fun init() {
        addSessionStep(AddRoleStates.STEP_1_CHOOSE_USER.step, this::stepOneChooseUser)
        addSessionStep(AddRoleStates.STEP_2_CHOOSE_ROLE.step, this::stepTwoRoleSet)
    }

    private fun stepOneChooseUser(user: User, bot: Bot, update: Update): Session {
        val username = update.message().replaceFirst("/", "")
        userRepository.findByUsername(username) ?: throw IllegalArgumentException("Не найден $username")
        bot.sendMessage(
            update.chatIdUnsafe(),
            enumValues<UserRoles>().joinToString(
                prefix = "Выбран пользователь ${username}, какую роль добавить?\n",
                separator = "\n",
                transform = { "/${it.name}" }
            )
        )
        return Session(
            sessionKey = AddRoleStates.STEP_2_CHOOSE_ROLE.step,
            session = objectMapper.writeValueAsString(AddRoleSession(username = username))
        )
    }

    private fun stepTwoRoleSet(user: User, bot: Bot, update: Update): Session {
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
        return emptySession
    }

    enum class AddRoleStates(
        val step: String,
    ) {
        STEP_1_CHOOSE_USER("${ADD_USER_ROLE}1"),
        STEP_2_CHOOSE_ROLE("${ADD_USER_ROLE}2"),
    }

    companion object {
        const val ADD_USER_ROLE = "add_user_role"
    }

    data class AddRoleSession(
        val username: String,
    )
}
