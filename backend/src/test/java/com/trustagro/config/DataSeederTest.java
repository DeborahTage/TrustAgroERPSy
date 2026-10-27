package com.trustagro.config;

import com.trustagro.masterdata.entity.MasterDataCategory;
import com.trustagro.masterdata.entity.MasterDataItem;
import com.trustagro.masterdata.repository.MasterDataItemRepository;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests verifying that {@link DataSeeder}:
 * 1. Honors {@code app.seed.demo-users=false} and {@code app.seed.demo-master-data=false}.
 * 2. Runs {@code seedMasterData()} only when {@code seedDemoMasterData=true}.
 * 3. Does NOT create demo users or master data when both flags are disabled.
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MasterDataItemRepository masterDataRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private final Map<String, MasterDataItem> masterDataDb = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(masterDataRepository.existsByCategoryAndValue(any(MasterDataCategory.class), anyString()))
                .thenAnswer(inv -> masterDataDb.containsKey(inv.getArgument(0) + ":" + inv.getArgument(1)));
        lenient().when(masterDataRepository.save(any(MasterDataItem.class)))
                .thenAnswer(inv -> {
                    MasterDataItem item = inv.getArgument(0);
                    masterDataDb.put(item.getCategory() + ":" + item.getValue(), item);
                    return item;
                });
    }

    @Test
    void defaultModeDisablesAllDemoSeeding() {
        DataSeeder seeder = new DataSeeder(
                userRepository, passwordEncoder, masterDataRepository, jdbcTemplate, false, false);

        seeder.run();

        verifyNoInteractions(userRepository);
        verify(masterDataRepository, never()).save(any());
        assertEquals(0, masterDataDb.size());
    }

    @Test
    void explicitDemoMasterDataFlagSeedsMasterDataWithoutDemoUsers() {
        DataSeeder seeder = new DataSeeder(
                userRepository, passwordEncoder, masterDataRepository, jdbcTemplate, false, true);

        seeder.run();

        verifyNoInteractions(userRepository);
        verify(masterDataRepository, atLeastOnce()).save(any());
        assertFalse(masterDataDb.isEmpty());
        assertTrue(masterDataDb.containsKey(MasterDataCategory.BREED + ":COBB_500"));
    }
}
