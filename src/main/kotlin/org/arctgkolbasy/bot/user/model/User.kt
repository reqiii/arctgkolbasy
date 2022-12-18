package org.arctgkolbasy.bot.user.model

import jakarta.persistence.*

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
    var isBot: Boolean,
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
    var roles: MutableSet<Role>,
) {
    constructor() : this(
        id = -1,
        telegramId = 0,
        isBot = false,
        firstName = "",
        roles = mutableSetOf(),
    )
}
