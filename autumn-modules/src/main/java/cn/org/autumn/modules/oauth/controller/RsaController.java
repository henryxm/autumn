package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.model.*;
import cn.org.autumn.model.Error;
import cn.org.autumn.service.AesService;
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
    private AesService aesService;

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
     * 获取AES加密密钥
     * 客户端调用此接口获取AES密钥和向量，用于后续的数据加密传输
     * AES密钥和向量使用客户端公钥进行RSA加密后返回
     * 客户端需要使用自己的私钥解密获取AES密钥和向量
     *
     * @param request 请求参数，包含uuid字段
     * @return AES密钥响应（包含加密后的密钥、向量和过期时间）
     */
    @PostMapping("/aes-key")
    public Response<AesKeyResponse> getAesKey(@Valid @RequestBody AesKeyRequest request, HttpServletRequest servlet) {
        try {
            String uuid = request.getUuid();
            // 检查客户端公钥是否存在
            if (!rsaService.hasValidClientPublicKey(uuid)) {
                return Response.error(Error.RSA_CLIENT_PUBLIC_KEY_NOT_FOUND);
            }
            // 生成或获取AES密钥
            AesKey aesKey = aesService.generate(uuid);
            // 使用客户端公钥加密AES密钥和向量
            String encryptedKey = rsaService.encrypt(aesKey.getKey(), uuid);
            String encryptedVector = rsaService.encrypt(aesKey.getVector(), uuid);
            // 构建返回结果
            AesKeyResponse response = new AesKeyResponse();
            response.setKey(encryptedKey);
            response.setVector(encryptedVector);
            response.setUuid(uuid);
            response.setExpireTime(aesKey.getExpireTime());
            response.setMessage("AES密钥获取成功");
            if (log.isDebugEnabled()) {
                log.debug("获取AES密钥，UUID: {}, 过期时间: {}, IP: {}", uuid, aesKey.getExpireTime(), IPUtils.getIp(servlet));
            }
            return Response.ok(response);
        } catch (cn.org.autumn.exception.CodeException e) {
            log.error("获取AES密钥失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        } catch (Exception e) {
            log.error("获取AES密钥失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet), e);
            return Response.error(e);
        }
    }
}
