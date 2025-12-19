package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.model.*;
import cn.org.autumn.service.RsaService;
import cn.org.autumn.utils.IPUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * RSA加密接口控制器
 * 提供RSA密钥对管理和加密解密功能
 *
 * @author Autumn
 */
@Slf4j
@RestController
@RequestMapping("/rsa/api/v1")
public class RsaController {

    @Autowired
    private RsaService rsaService;

    @Autowired
    Gson gson;

    /**
     * 获取服务端公钥
     * 客户端调用此接口获取服务端的RSA公钥，用于加密数据发送给服务端
     * 客户端需要提交自己生成的UUID，服务端使用此UUID关联存储密钥对
     *
     * @param request 请求参数，包含uuid字段
     * @return 服务端公钥信息（包含公钥、UUID、过期时间）
     */
    @PostMapping("/public-key")
    public Response<PublicKey> getPublicKey(@Valid @RequestBody PublicKeyRequest request, HttpServletRequest servlet) {
        try {
            // 使用客户端提交的UUID获取或生成服务端密钥对
            KeyPair keyPair = rsaService.getKeyPair(request.getUuid());
            // 转换为PublicKey对象返回给客户端
            PublicKey publicKey = keyPair.toPublicKey();
            if (log.isDebugEnabled()) {
                log.debug("获取公钥，UUID:{}, 过期时间:{}, IP:{}", publicKey.getUuid(), publicKey.getExpireTime(), IPUtils.getIp(servlet));
            }
            return Response.ok(publicKey);
        } catch (Exception e) {
            log.error("获取公钥:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        }
    }

    /**
     * 上传客户端公钥
     * 客户端生成RSA密钥对后，将公钥上传给服务端保存
     * 服务端后续可以使用此公钥加密数据返回给客户端
     * 客户端需要使用与获取服务端公钥时相同的UUID
     *
     * @param request 请求参数，包含uuid和publicKey字段
     * @return 保存结果（包含客户端标识、过期时间等信息）
     */
    @PostMapping("/client/public-key")
    public Response<ClientPublicKeyResponse> uploadPublicKey(@Valid @RequestBody ClientPublicKeyRequest request, HttpServletRequest servlet) {
        try {
            // 使用客户端提交的UUID和过期时间保存客户端公钥
            // 如果客户端提供了过期时间则使用客户端的，否则使用后端默认的
            ClientPublicKey clientPublicKey = rsaService.savePublicKey(request.getUuid(), request.getPublicKey(), request.getExpireTime());
            // 构建返回结果
            ClientPublicKeyResponse response = new ClientPublicKeyResponse();
            response.setUuid(clientPublicKey.getUuid());
            response.setExpireTime(clientPublicKey.getExpireTime());
            response.setCreateTime(clientPublicKey.getCreateTime());
            response.setMessage("客户端上传保存成功");
            if (log.isDebugEnabled()) {
                log.debug("上传公钥，UUID: {}, 过期时间: {}, IP:{}", clientPublicKey.getUuid(), clientPublicKey.getExpireTime(), IPUtils.getIp(servlet));
            }
            return Response.ok(response);
        } catch (Exception e) {
            log.error("上传公钥:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        }
    }

    /**
     * 使用客户端公钥加密数据
     * 服务端使用客户端的公钥加密数据，返回给客户端
     * 客户端使用自己的私钥解密
     *
     * @param request 请求参数，包含uuid和data字段
     * @return 加密后的数据（Base64编码）
     */
    @PostMapping("/client/encrypt")
    public Response<EncryptDataResponse> encryptWithClientPublicKey(@Valid @RequestBody EncryptDataWithUuidRequest request, HttpServletRequest servlet) {
        try {
            // 使用客户端提交的UUID和客户端公钥加密数据
            String encryptedData = rsaService.encrypt(request.getData(), request.getUuid());
            // 构建返回结果
            EncryptDataResponse response = new EncryptDataResponse();
            response.setEncrypted(encryptedData);
            response.setMessage("数据加密成功");
            return Response.ok(response);
        } catch (Exception e) {
            log.error("加密失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        }
    }

    /**
     * 检查客户端公钥是否存在且有效
     *
     * @param request 请求参数，包含uuid字段
     * @return 检查结果
     */
    @PostMapping(value = "/client/public-key/status")
    public Response<ClientPublicKeyStatusResponse> checkClientPublicKeyStatus(@Valid @RequestBody ClientPublicKeyStatusRequest request, HttpServletRequest servlet) {
        try {
            String uuid = request.getUuid();
            boolean hasValidKey = rsaService.hasValidClientPublicKey(uuid);
            ClientPublicKey clientPublicKey = rsaService.getClientPublicKey(uuid);
            ClientPublicKeyStatusResponse response = new ClientPublicKeyStatusResponse();
            response.setHasValidKey(hasValidKey);
            response.setUuid(uuid);
            if (clientPublicKey != null) {
                response.setExpireTime(clientPublicKey.getExpireTime());
                response.setCreateTime(clientPublicKey.getCreateTime());
                response.setIsExpired(clientPublicKey.isExpired());
            }
            return Response.ok(response);
        } catch (Exception e) {
            log.error("状态失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        }
    }
}
