package com.trustagro.qa.fixture;

import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.entity.CustomerType;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.entity.ItemStatus;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Lightweight entity builders for Step 14 golden scenarios (test-only).
 */
public final class QaEntities {

    private QaEntities() {}

    public static User user(Long id, String email, String name, Role role) {
        User u = new User();
        setId(u, id);
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setPassword("hashed");
        return u;
    }

    public static CrmClient client(Long id, String name) {
        CrmClient c = new CrmClient();
        setId(c, id);
        c.setClientName(name);
        c.setStatus(ClientStatus.ACTIVE);
        c.setCustomerType(CustomerType.FARM);
        return c;
    }

    public static Farm farm(Long id, String name) {
        Farm f = new Farm();
        setId(f, id);
        f.setFarmName(name);
        f.setStatus(FarmStatus.ACTIVE);
        return f;
    }

    public static Flock flock(Long id, Farm farm, String batch, int Chickens) {
        Flock fl = new Flock();
        setId(fl, id);
        fl.setFarm(farm);
        fl.setBatchCode(batch);
        fl.setStatus(FlockStatus.ACTIVE);
        fl.setCurrentBirdCount(Chickens);
        fl.setInitialBirdCount(Chickens);
        return fl;
    }

    public static Supplier supplier(Long id, String code, String name) {
        Supplier s = new Supplier();
        setId(s, id);
        s.setSupplierCode(code);
        s.setName(name);
        s.setPaymentTerms("NET_30");
        s.setActive(true);
        return s;
    }

    public static InventoryItem item(Long id, String sku, String name, double stock, BigDecimal cost) {
        InventoryItem item = new InventoryItem();
        setId(item, id);
        item.setSku(sku);
        item.setItemName(name);
        item.setCurrentStock(stock);
        item.setAvgUnitCost(cost);
        item.setLastPurchasePrice(cost);
        item.setSellingPrice(cost.multiply(new BigDecimal("1.50")));
        item.setRetailPrice(cost.multiply(new BigDecimal("1.50")));
        item.setStatus(ItemStatus.ACTIVE);
        return item;
    }

    public static void setId(Object entity, Long id) {
        Class<?> c = entity.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField("id");
                f.setAccessible(true);
                f.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("No id field on " + entity.getClass());
    }
}
