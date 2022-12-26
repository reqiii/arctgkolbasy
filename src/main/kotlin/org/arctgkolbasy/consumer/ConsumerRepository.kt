package org.arctgkolbasy.consumer

import org.springframework.data.repository.CrudRepository

interface ConsumerRepository: CrudRepository<Consumer, ConsumerId>
