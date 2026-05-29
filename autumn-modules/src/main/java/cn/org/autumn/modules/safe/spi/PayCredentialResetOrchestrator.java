package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 重置身份校验：优先自定义 SPI，否则登录密码。
 */
@Component
public class PayCredentialResetOrchestrator {

    @Autowired(required = false)
    private List<PayCredentialResetVerifier> verifiers;

    @Autowired
    private LoginPasswordPayResetVerifier loginPasswordPayResetVerifier;

    public void verify(PayResetContext ctx) throws CodeException {
        if (ctx == null)
            throw new CodeException(Error.OPERATION_NOT_ALLOWED);
        List<PayCredentialResetVerifier> chain = new ArrayList<>();
        if (verifiers != null) {
            for (PayCredentialResetVerifier verifier : verifiers) {
                if (verifier != null && !(verifier instanceof LoginPasswordPayResetVerifier))
                    chain.add(verifier);
            }
            chain.sort(Comparator.comparingInt(v -> {
                Order order = v.getClass().getAnnotation(Order.class);
                return order == null ? 0 : order.value();
            }));
        }
        for (PayCredentialResetVerifier verifier : chain) {
            if (verifier.supports(ctx)) {
                verifier.verifyReset(ctx);
                return;
            }
        }
        if (loginPasswordPayResetVerifier.supports(ctx)) {
            loginPasswordPayResetVerifier.verifyReset(ctx);
            return;
        }
        throw new CodeException(Error.OPERATION_NOT_ALLOWED);
    }
}
