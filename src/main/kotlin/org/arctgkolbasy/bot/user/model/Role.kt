package org.arctgkolbasy.bot.user.model

import jakarta.persistence.*
import org.arctgkolbasy.bot.user.UserRoles

@Entity
@Table(name = "roles")
class Role (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", insertable = false)
    val id: Long,
    @Column(name = "role_name")
    @Enumerated(EnumType.STRING)
    var roleName: UserRoles,
    @ManyToMany(mappedBy = "roles")
    var users: List<User>
) {
    constructor() : this(
        -1,
        UserRoles.GUEST,
        listOf(),
    )
}
