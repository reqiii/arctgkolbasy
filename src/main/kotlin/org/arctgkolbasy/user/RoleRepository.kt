package org.arctgkolbasy.user

import org.arctgkolbasy.bot.user.UserRoles
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository: CrudRepository<Role, Long> {
    fun findByRoleName(userRoles: UserRoles): Role?
}
