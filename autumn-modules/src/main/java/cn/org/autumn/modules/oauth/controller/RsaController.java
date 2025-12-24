package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.exception.CodeException;
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
import java.util.*;

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
    @Endpoint(hidden = true)
    public Response<RsaKey> getPublicKey(@Valid @RequestBody Request<?> request, HttpServletRequest servlet) {
        try {
            // 使用客户端提交的UUID获取或生成服务端密钥对
            RsaKey rsaKey = rsaService.getRsaKey(request.getSession());
            // 转换为PublicKey对象返回给客户端
            RsaKey copy = rsaKey.copy();
            if (log.isDebugEnabled()) {
                log.debug("获取公钥，UUID:{}, 过期时间:{}, IP:{}", copy.getSession(), copy.getExpireTime(), IPUtils.getIp(servlet));
            }
            return Response.ok(copy);
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
    @Endpoint(hidden = true)
    public Response<RsaKey> uploadPublicKey(@Valid @RequestBody RsaKey request, HttpServletRequest servlet) {
        try {
            // 使用客户端提交的UUID和过期时间保存客户端公钥
            // 如果客户端提供了过期时间则使用客户端的，否则使用后端默认的
            RsaKey clientPublicKey = rsaService.savePublicKey(request.getSession(), request.getPublicKey(), request.getExpireTime());
            // 构建返回结果
            RsaKey response = new RsaKey();
            response.setSession(clientPublicKey.getSession());
            response.setExpireTime(clientPublicKey.getExpireTime());
            if (log.isDebugEnabled()) {
                log.debug("上传公钥，UUID: {}, 过期时间: {}, IP:{}", clientPublicKey.getSession(), clientPublicKey.getExpireTime(), IPUtils.getIp(servlet));
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
    @Endpoint(hidden = true)
    public Response<AesKey> getAesKey(@Valid @RequestBody Request<?> request, HttpServletRequest servlet) {
        try {
            String uuid = request.getSession();
            // 检查客户端公钥是否存在
            if (!rsaService.hasValidClientPublicKey(uuid)) {
                return Response.error(Error.RSA_CLIENT_PUBLIC_KEY_NOT_FOUND);
            }
            // 生成或获取AES密钥
            AesKey aesKey = aesService.getAesKey(uuid);
            // 使用客户端公钥加密AES密钥和向量
            String encryptedKey = rsaService.encrypt(aesKey.getKey(), uuid);
            String encryptedVector = rsaService.encrypt(aesKey.getVector(), uuid);
            // 构建返回结果
            AesKey response = new AesKey();
            response.setKey(encryptedKey);
            response.setVector(encryptedVector);
            response.setSession(uuid);
            response.setExpireTime(aesKey.getExpireTime());
            return Response.ok(response);
        } catch (cn.org.autumn.exception.CodeException e) {
            log.error("获取AES密钥失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        } catch (Exception e) {
            log.error("获取AES密钥失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet), e);
            return Response.error(e);
        }
    }

    /**
     * 初始化加密（合并接口）
     * 客户端提交自己的公钥，服务端一次性返回：
     * 1. 服务端公钥（用于客户端加密数据发送给服务端）
     * 2. 加密后的AES密钥和向量（使用客户端公钥加密，客户端使用私钥解密）
     * <p>
     * 此接口合并了以下三个接口的功能：
     * - /public-key: 获取服务端公钥
     * - /client/public-key: 上传客户端公钥
     * - /aes-key: 获取AES密钥
     *
     * @param request 请求参数，包含uuid和clientPublicKey字段
     * @return 初始化加密响应（包含服务端公钥和加密后的AES密钥）
     */
    @PostMapping("/init")
    public Response<Encryption> initEncryption(@Valid @RequestBody Request<InitRequest> request, HttpServletRequest servlet) {
        try {
            InitRequest initRequest = request.getData();
            String uuid = request.getSession();
            String clientPublicKey = initRequest.getPublicKey();
            Long expireTime = initRequest.getExpireTime();
            // 1. 生成或获取服务端密钥对
            RsaKey serverKeyPair = rsaService.getRsaKey(uuid);
            RsaKey ras = serverKeyPair.copy();
            // 2. 保存客户端公钥
            rsaService.savePublicKey(uuid, clientPublicKey, expireTime);
            // 3. 生成或获取AES密钥
            AesKey aesKey = aesService.getAesKey(uuid);
            AesKey aes = new AesKey();
            aes.setSession(uuid);
            aes.setKey(rsaService.encrypt(aesKey.getKey(), uuid));
            aes.setVector(rsaService.encrypt(aesKey.getVector(), uuid));
            aes.setExpireTime(aesKey.getExpireTime());
            Encryption response = new Encryption();
            response.setRas(ras);
            response.setAes(aes);
            List<EndpointInfo> endpoints = rsaService.getEncryptEndpoints();
            String json = gson.toJson(endpoints);
            String end = aesService.encrypt(json, uuid);
            response.setEndpoints(end);
            return Response.ok(response);
        } catch (CodeException e) {
            log.error("初始化失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet));
            return Response.error(e);
        } catch (Exception e) {
            log.error("初始化失败:{}, IP:{}", e.getMessage(), IPUtils.getIp(servlet), e);
            return Response.error(e);
        }
    }

    /**
     * 解析所有RestController接口，找出请求body类型和返回值类型是Encrypt的实例
     *
     * @return 包含请求body和返回值类型为Encrypt的接口信息列表
     */
    @RequestMapping(value = "/endpoints", method = {RequestMethod.POST, RequestMethod.GET})
    public Response<List<EndpointInfo>> getEncryptEndpoints(@Valid @RequestBody(required = false) Request<?> request, HttpServletRequest servlet) {
        try {
            List<EndpointInfo> endpoints = rsaService.getEncryptEndpoints();
            return Response.ok(endpoints);
        } catch (Exception e) {
            log.error("扫描接口: {}", e.getMessage(), e);
            return Response.error(e);
        }
    }
}
