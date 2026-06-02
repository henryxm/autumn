package cn.org.autumn.modules.safe.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.safe.dto.*;
import cn.org.autumn.modules.safe.entity.PayUserSecuritySettingEntity;
import cn.org.autumn.modules.safe.entity.PayUserTrustedDeviceEntity;
import cn.org.autumn.modules.safe.entity.PayUserTrustedIpEntity;
import cn.org.autumn.modules.safe.service.PayGateService;
import cn.org.autumn.modules.safe.service.PayUserBiometricService;
import cn.org.autumn.modules.safe.service.PayUserGestureService;
import cn.org.autumn.modules.safe.service.PayUserPinService;
import cn.org.autumn.modules.safe.service.PayUserSecuritySettingService;
import cn.org.autumn.modules.safe.service.PayUserTrustedDeviceService;
import cn.org.autumn.modules.safe.service.PayUserTrustedIpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 支付密码、手势与生物识别<strong>开放 API</strong>（面向 App / H5 / 业务网关）。
 *
 * <p><b>基础路径</b>：{@code POST /safe/api/v1/*}（全部为 POST，JSON 体）</p>
 *
 * <p><b>鉴权</b>：{@link Authenticated} + 用户访问令牌（请求头 {@code Token} 或
 * {@code Authorization: Bearer ...}）；禁止机器人 {@code rbt_} 令牌。</p>
 *
 * <p><b>报文</b>：统一 {@link Request} / {@link Response}；业务字段在 {@code data} 内。
 * 成功 {@code code == 0}；鉴权失败多为 {@code -10000}；业务错误 {@code 850～864}。</p>
 *
 * <p><b>支付闸门</b>：扣款前先 {@code POST /gate/assess}，再按 {@code authMode} 免密或
 * {@code pin/verify} / {@code biometric/verify}（须带 {@code gateToken} 与 {@code amountCent}）。</p>
 *
 * <p><b>完整字段说明、示例 JSON、错误码与对接流程</b>见仓库文档
 * {@code docs/AI_SAFE_CREDENTIAL.md}。</p>
 *
 * <p><b>服务端扣款</b>：业务仓注入 {@link cn.org.autumn.pay.PayPinVerifier}，在
 * {@code pin/verify} 或 {@code biometric/verify} 返回的 {@code verifyToken} 有效期内调用
 * {@code requireVerifyToken}，或直接 {@code requireVerified(userUuid, pin)}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/safe/api/v1")
public class PayCredentialApiController {

    @Autowired
    private PayUserPinService payUserPinService;

    @Autowired
    private PayUserGestureService payUserGestureService;

    @Autowired
    private PayUserBiometricService payUserBiometricService;

    @Autowired
    private PayGateService payGateService;

    @Autowired
    private PayUserSecuritySettingService payUserSecuritySettingService;

    @Autowired
    private PayUserTrustedDeviceService payUserTrustedDeviceService;

    @Autowired
    private PayUserTrustedIpService payUserTrustedIpService;

    /**
     * 查询当前用户支付密码是否已设置、是否锁定及剩余尝试次数。
     *
     * @param request 可省略 {@code data}
     * @return {@link PayPinStatusResult}
     * @see #pinSet(Request, UserContext, HttpServletRequest)
     */
    @PostMapping("/pin/status")
    @Authenticated
    public Response<PayPinStatusResult> pinStatus(@Valid @RequestBody(required = false) Request<?> request, UserContext context) {
        try {
            return Response.ok(payUserPinService.status(requireUser(context)));
        } catch (Exception e) {
            log.debug("支付密码状态查询失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 首次设置支付密码（须与 {@code confirm} 一致；位数与弱密码规则见 {@code PAY_CREDENTIAL_CONFIG}）。
     *
     * @param request {@link PayPinSetRequest}：{@code pin}、{@code confirm}
     */
    @PostMapping("/pin/set")
    @Authenticated
    public Response<String> pinSet(@Valid @RequestBody Request<PayPinSetRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayPinSetRequest data = data(request);
            payUserPinService.setPin(requireUser(context), data.getPin(), data.getConfirm(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("设置支付密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 已知原密码时修改支付密码。
     *
     * @param request {@link PayPinChangeRequest}：{@code oldPin}、{@code newPin}、{@code confirm}
     */
    @PostMapping("/pin/change")
    @Authenticated
    public Response<String> pinChange(@Valid @RequestBody Request<PayPinChangeRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayPinChangeRequest data = data(request);
            payUserPinService.changePin(requireUser(context), data.getOldPin(), data.getNewPin(), data.getConfirm(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("修改支付密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 忘记支付密码时重置；须通过重置身份校验（默认 {@code loginPassword}，可扩展 SPI 支持 {@code smsCode} 等）。
     *
     * @param request {@link PayPinResetRequest}
     */
    @PostMapping("/pin/reset")
    @Authenticated
    public Response<String> pinReset(@Valid @RequestBody Request<PayPinResetRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayPinResetRequest data = data(request);
            payUserPinService.resetPin(requireUser(context), data.getNewPin(), data.getConfirm(), data.getLoginPassword(), data.getSmsCode(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("重置支付密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 校验支付密码；成功返回一次性 {@link PayPinVerifyResult#getVerifyToken()}（有效分钟见配置）。
     *
     * @param request {@link PayPinVerifyRequest}：{@code pin}、{@code gateToken}、{@code amountCent}
     */
    @PostMapping("/pin/verify")
    @Authenticated
    public Response<PayPinVerifyResult> pinVerify(@Valid @RequestBody Request<PayPinVerifyRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayPinVerifyRequest data = data(request);
            return Response.ok(payUserPinService.verifyPin(requireUser(context), data.getPin(), data.getGateToken(), data.getAmountCent(), servlet));
        } catch (Exception e) {
            log.debug("校验支付密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 查询手势密码状态（字段含义同 {@link #pinStatus(Request, UserContext)}）。
     */
    @PostMapping("/gesture/status")
    @Authenticated
    public Response<PayGestureStatusResult> gestureStatus(@Valid @RequestBody(required = false) Request<?> request, UserContext context) {
        try {
            return Response.ok(payUserGestureService.status(requireUser(context)));
        } catch (Exception e) {
            log.debug("手势密码状态查询失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 设置九宫格手势；{@code points} 为 0～8 的有序轨迹，{@code confirmPoints} 须一致。
     *
     * @param request {@link PayGestureSetRequest}
     */
    @PostMapping("/gesture/set")
    @Authenticated
    public Response<String> gestureSet(@Valid @RequestBody Request<PayGestureSetRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayGestureSetRequest data = data(request);
            payUserGestureService.setGesture(requireUser(context), data.getPoints(), data.getConfirmPoints(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("设置手势密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 修改手势密码。
     *
     * @param request {@link PayGestureChangeRequest}
     */
    @PostMapping("/gesture/change")
    @Authenticated
    public Response<String> gestureChange(@Valid @RequestBody Request<PayGestureChangeRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayGestureChangeRequest data = data(request);
            payUserGestureService.changeGesture(requireUser(context), data.getOldPoints(), data.getNewPoints(), data.getConfirmPoints(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("修改手势密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 忘记手势时重置（默认校验登录密码 {@code loginPassword}）。
     *
     * @param request {@link PayGestureResetRequest}
     */
    @PostMapping("/gesture/reset")
    @Authenticated
    public Response<String> gestureReset(@Valid @RequestBody Request<PayGestureResetRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayGestureResetRequest data = data(request);
            payUserGestureService.resetGesture(requireUser(context), data.getPoints(), data.getConfirmPoints(), data.getLoginPassword(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("重置手势密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 校验手势；成功返回 {@link PayPinVerifyResult}（与 PIN verify 共用 verifyToken 机制）。
     *
     * @param request {@link PayGestureVerifyRequest}：{@code points}
     */
    @PostMapping("/gesture/verify")
    @Authenticated
    public Response<PayPinVerifyResult> gestureVerify(@Valid @RequestBody Request<PayGestureVerifyRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayGestureVerifyRequest data = data(request);
            return Response.ok(payUserGestureService.verifyGesture(requireUser(context), data.getPoints(), data.getGateToken(), data.getAmountCent(), servlet));
        } catch (Exception e) {
            log.debug("校验手势密码失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 注册或更新设备生物公钥（Base64 X.509 SPKI）；同一 {@code deviceId} 覆盖更新。
     *
     * @param request {@link PayBiometricRegisterRequest}
     */
    @PostMapping("/biometric/register")
    @Authenticated
    public Response<String> biometricRegister(@Valid @RequestBody Request<PayBiometricRegisterRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayBiometricRegisterRequest data = data(request);
            payUserBiometricService.register(requireUser(context), data.getDeviceId(), data.getPlatform(), data.getCredentialId(), data.getPublicKey(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("注册生物识别失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 列出当前用户已绑定的生物设备（不含公钥）。
     */
    @PostMapping("/biometric/list")
    @Authenticated
    public Response<List<PayBiometricDeviceView>> biometricList(@Valid @RequestBody(required = false) Request<?> request, UserContext context) {
        try {
            return Response.ok(payUserBiometricService.listDevices(requireUser(context)));
        } catch (Exception e) {
            log.debug("生物识别设备列表失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 吊销指定设备的生物绑定。
     *
     * @param request {@link PayBiometricDeviceRequest}：{@code deviceId}
     */
    @PostMapping("/biometric/revoke")
    @Authenticated
    public Response<String> biometricRevoke(@Valid @RequestBody Request<PayBiometricDeviceRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayBiometricDeviceRequest data = data(request);
            payUserBiometricService.revoke(requireUser(context), data.getDeviceId(), servlet);
            return Response.ok();
        } catch (Exception e) {
            log.debug("吊销生物识别失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 签发生物验签挑战串；客户端对 challenge 做 SHA256withRSA 签名后调 {@link #biometricVerify}。
     *
     * @param request {@link PayBiometricDeviceRequest}：{@code deviceId}
     * @return {@link PayBiometricChallengeResult}
     */
    @PostMapping("/biometric/challenge")
    @Authenticated
    public Response<PayBiometricChallengeResult> biometricChallenge(@Valid @RequestBody Request<PayBiometricDeviceRequest> request, UserContext context) {
        try {
            PayBiometricDeviceRequest data = data(request);
            return Response.ok(payUserBiometricService.challenge(requireUser(context), data.getDeviceId()));
        } catch (Exception e) {
            log.debug("生物识别挑战失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 校验生物签名；成功返回 {@link PayPinVerifyResult#getVerifyToken()}。
     *
     * @param request {@link PayBiometricVerifyRequest}：{@code deviceId}、{@code challenge}、{@code signature}
     */
    @PostMapping("/biometric/verify")
    @Authenticated
    public Response<PayPinVerifyResult> biometricVerify(@Valid @RequestBody Request<PayBiometricVerifyRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayBiometricVerifyRequest data = data(request);
            return Response.ok(payUserBiometricService.verify(requireUser(context), data.getDeviceId(), data.getChallenge(), data.getSignature(), data.getGateToken(), data.getAmountCent(), servlet));
        } catch (Exception e) {
            log.debug("生物识别验签失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 支付安全设置与常用设备/IP 状态（含生效的免密阈值与窗口）。
     */
    @PostMapping("/security/status")
    @Authenticated
    public Response<PaySecurityStatusResult> securityStatus(@Valid @RequestBody(required = false) Request<?> request, UserContext context) {
        try {
            return Response.ok(payGateService.securityStatus(requireUser(context)));
        } catch (Exception e) {
            log.debug("支付安全状态查询失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 更新用户支付安全偏好（免密开关、免密金额上限分、免密窗口分钟；0 表示沿用全局）。
     */
    @PostMapping("/security/settings/update")
    @Authenticated
    public Response<PayUserSecuritySettingEntity> securitySettingsUpdate(@Valid @RequestBody Request<PaySecuritySettingsUpdateRequest> request, UserContext context) {
        try {
            PaySecuritySettingsUpdateRequest data = data(request);
            return Response.ok(payUserSecuritySettingService.saveUserSettings(requireUser(context), data.getPasswordlessEnabled(), data.getPasswordlessMaxAmountCent(), data.getPasswordlessWindowMinutes(), data.getGesturePaymentEnabled()));
        } catch (Exception e) {
            log.debug("更新支付安全设置失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 将设备标记为常用支付设备。
     */
    @PostMapping("/security/device/trust")
    @Authenticated
    public Response<String> securityDeviceTrust(@Valid @RequestBody Request<PayTrustedDeviceRequest> request, UserContext context) {
        try {
            PayTrustedDeviceRequest data = data(request);
            payUserTrustedDeviceService.trust(requireUser(context), data.getDeviceId(), data.getDeviceName(), data.getPlatform());
            return Response.ok();
        } catch (Exception e) {
            log.debug("信任支付设备失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 取消常用支付设备。
     */
    @PostMapping("/security/device/untrust")
    @Authenticated
    public Response<String> securityDeviceUntrust(@Valid @RequestBody Request<PayTrustedDeviceRequest> request, UserContext context) {
        try {
            PayTrustedDeviceRequest data = data(request);
            payUserTrustedDeviceService.untrust(requireUser(context), data.getDeviceId());
            return Response.ok();
        } catch (Exception e) {
            log.debug("取消信任支付设备失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 将 IP 标记为常用支付网络（{@code ip} 为空时使用当前请求 IP）。
     */
    @PostMapping("/security/ip/trust")
    @Authenticated
    public Response<String> securityIpTrust(@Valid @RequestBody Request<PayTrustedIpRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayTrustedIpRequest data = data(request);
            String ip = data.getIp();
            if (org.apache.commons.lang3.StringUtils.isBlank(ip))
                ip = cn.org.autumn.utils.IPUtils.getIp(servlet);
            payUserTrustedIpService.trust(requireUser(context), ip, data.getLocationLabel());
            return Response.ok();
        } catch (Exception e) {
            log.debug("信任支付IP失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 取消常用支付 IP。
     */
    @PostMapping("/security/ip/untrust")
    @Authenticated
    public Response<String> securityIpUntrust(@Valid @RequestBody Request<PayTrustedIpRequest> request, UserContext context) {
        try {
            PayTrustedIpRequest data = data(request);
            payUserTrustedIpService.untrust(requireUser(context), data.getIp());
            return Response.ok();
        } catch (Exception e) {
            log.debug("取消信任支付IP失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    /**
     * 支付闸门：输入支付密码前综合评估环境、金额、设备、IP、短时同额等；成功返回 {@code gateToken}。
     */
    @PostMapping("/gate/assess")
    @Authenticated
    public Response<PayGateAssessResult> gateAssess(@Valid @RequestBody Request<PayGateAssessRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            PayGateAssessRequest data = data(request);
            return Response.ok(payGateService.assess(requireUser(context), data, servlet));
        } catch (Exception e) {
            log.debug("支付闸门评估失败: {}", e.getMessage());
            return Response.error(e);
        }
    }

    private <T> T data(Request<T> request) throws CodeException {
        if (request == null || request.getData() == null)
            throw new CodeException("请求体不能为空");
        return request.getData();
    }

    private String requireUser(UserContext context) throws CodeException {
        if (context == null)
            throw new CodeException("请登录", -10000);
        if (context.isRobot())
            throw new CodeException("请使用用户令牌", -10000);
        if (org.apache.commons.lang3.StringUtils.isBlank(context.getUuid()))
            throw new CodeException("用户不可用", -10000);
        return context.getUuid();
    }
}
