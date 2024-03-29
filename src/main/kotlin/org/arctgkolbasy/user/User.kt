package org.arctgkolbasy.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

@Entity
@Table(name = "users")
@NamedEntityGraph(
    name = "User.detail",
    attributeNodes = [NamedAttributeNode("roles")]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", insertable = false)
    val id: Long,
    @Column(name = "telegram_id")
    val telegramId: Long,
    @Column(name = "is_bot")
    val isBot: Boolean,
    @Column(name = "firstname")
    var firstName: String,
    @Column(name = "lastname")
    var lastName: String? = null,
    @Column(name = "username")
    var username: String? = null,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "users_to_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<Role>,
    @Column(name = "session_key")
    var sessionKey: String?,
    @Column(name = "session")
    var session: String?,
    @Column(name = "telegram_chat_id")
    var telegramChatId: Long?,
    @Column(name = "last_menu_message_id")
    var lastMenuMessageId: Long?,
)
