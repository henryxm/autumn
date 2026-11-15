package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class LegacyPasswordUpgradeSupportTest {

    @Test
    @SuppressWarnings("unchecked")
    void refusesWhenLegacyShaDoesNotMatchUserInput() {
        SysUserEntity row = new SysUserEntity();
        row.setUuid("u1");
        row.setSalt("abcdefghijklmnopqrst");
        row.setPassword(ShiroUtils.sha256("correct", row.getSalt()));
        BaseMapper<SysUserEntity> mapper = Mockito.mock(BaseMapper.class);
        assertFalse(LegacyPasswordUpgradeSupport.tryUpgradeAtomically(
                "u1", "wrong-password", row, mapper));
        Mockito.verify(mapper, Mockito.never()).update(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void upgradesOnlyWhenUserInputMatchesLegacySha() {
        SysUserEntity row = new SysUserEntity();
        row.setUuid("u1");
        row.setSalt("abcdefghijklmnopqrst");
        row.setPassword(ShiroUtils.sha256("correct", row.getSalt()));
        BaseMapper<SysUserEntity> mapper = Mockito.mock(BaseMapper.class);
        when(mapper.update(ArgumentMatchers.isNull(), ArgumentMatchers.any(Wrapper.class))).thenReturn(1);
        assertTrue(LegacyPasswordUpgradeSupport.tryUpgradeAtomically(
                "u1", "correct", row, mapper));
        assertTrue(PasswordHashSupport.isPbkdf2(row.getPassword()));
        assertTrue(PasswordHashSupport.matchesPbkdf2("correct", row.getPassword()));
        Mockito.verify(mapper, Mockito.times(1)).update(ArgumentMatchers.isNull(), ArgumentMatchers.any(Wrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void refusesBlankAndAlreadyPbkdf2() {
        SysUserEntity row = new SysUserEntity();
        row.setUuid("u1");
        row.setSalt("abcdefghijklmnopqrst");
        row.setPassword(PasswordHashSupport.hashNew("x"));
        BaseMapper<SysUserEntity> mapper = Mockito.mock(BaseMapper.class);
        assertFalse(LegacyPasswordUpgradeSupport.tryUpgradeAtomically("u1", "x", row, mapper));
        assertFalse(LegacyPasswordUpgradeSupport.tryUpgradeAtomically("u1", "", row, mapper));
        Mockito.verify(mapper, Mockito.never()).update(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void matchesLegacyDoesNotAcceptPbkdf2AsLegacy() {
        String pbk = PasswordHashSupport.hashNew("secret");
        assertFalse(PasswordHashSupport.matchesLegacySha256("secret", pbk, "salt"));
        assertTrue(PasswordHashSupport.matches("secret", pbk, "salt"));
    }

    @Test
    void hashNewFromVerifiedUserInputRejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHashSupport.hashNewFromVerifiedUserInput(""));
    }
}
