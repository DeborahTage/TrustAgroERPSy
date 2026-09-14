package com.trustagro.farm.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FlockRepositoryLockTest {

    @Test
    void findByIdForUpdateUsesPessimisticWriteLock() throws Exception {
        Method method = FlockRepository.class.getMethod("findByIdForUpdate", Long.class);
        Lock lock = method.getAnnotation(Lock.class);
        assertNotNull(lock, "findByIdForUpdate must declare @Lock for double-sale protection");
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }
}
