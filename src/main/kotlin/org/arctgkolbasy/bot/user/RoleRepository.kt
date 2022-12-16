package org.arctgkolbasy.bot.user

import org.arctgkolbasy.bot.user.model.Roles
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository: CrudRepository<Roles, Long> {
    fun findByRoleName(userRoles: UserRoles): Roles?
}
