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
import jakarta.persistence.Table

@Entity
@Table(name = "users")
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
) {
    constructor() : this(
        id = -1,
        telegramId = 0,
        isBot = false,
        firstName = "",
        roles = mutableSetOf(),
        sessionKey = null,
        session = null,
    )
}
