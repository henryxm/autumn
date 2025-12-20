package cn.org.autumn.model;

import lombok.Getter;

/**
 * 错误码枚举
 * 定义系统所有错误码和错误信息
 * <p>
 * 状态码范围：
 * - 0: API调用成功（Response.success()判断code==0为成功）
 * - 100-199: HTTP标准信息性状态码（RFC 7231）
 * - 200-299: HTTP标准成功状态码（RFC 7231）
 * - 300-399: HTTP标准重定向状态码（RFC 7231）
 * - 400-499: HTTP标准客户端错误（RFC 7231）
 * - 500-599: HTTP标准服务器错误（RFC 7231）
 * - 800-899: 业务逻辑错误
 *   - 814-849: 数据库相关错误
 * - 900-999: 认证授权错误
 * - 1000-1099: RSA加密相关错误（需要客户端执行相应操作）
 * - 1100-1199: AES加密相关错误（需要客户端执行相应操作）
 * - 100000+: 系统错误
 *
 * @author Autumn
 */
@Getter
public enum Error {
    // ==================== 成功状态码 ====================
    /**
     * 0 - API调用成功
     * 特指API调用成功，Response.success()方法判断code==0为成功
     */
    SUCCESS(0, "成功"),

    // ==================== HTTP标准信息性状态码 (100-199) ====================
    /**
     * 100 Continue - 继续
     * 服务器已收到请求头，客户端应继续发送请求体
     */
    CONTINUE(100, "继续"),

    /**
     * 101 Switching Protocols - 切换协议
     * 服务器正在按客户端请求切换协议
     */
    SWITCHING_PROTOCOLS(101, "切换协议"),

    /**
     * 102 Processing - 处理中
     * WebDAV扩展，服务器已收到并正在处理请求，但无响应可用
     */
    PROCESSING(102, "处理中"),

    /**
     * 103 Early Hints - 早期提示
     * 用于在最终HTTP消息之前返回一些响应头
     */
    EARLY_HINTS(103, "早期提示"),

    // ==================== HTTP标准成功状态码 (200-299) ====================
    /**
     * 200 OK - 请求成功
     * HTTP标准成功状态码，表示请求已成功处理
     */
    OK(200, "请求成功"),

    /**
     * 201 Created - 已创建
     * 请求已成功处理，并创建了新的资源
     */
    CREATED(201, "资源已创建"),

    /**
     * 202 Accepted - 已接受
     * 请求已接受处理，但处理尚未完成
     */
    ACCEPTED(202, "请求已接受，正在处理"),

    /**
     * 203 Non-Authoritative Information - 非权威信息
     * 服务器成功处理了请求，但返回的信息可能来自另一个来源
     */
    NON_AUTHORITATIVE_INFORMATION(203, "非权威信息"),

    /**
     * 204 No Content - 无内容
     * 服务器成功处理了请求，但不返回任何内容
     */
    NO_CONTENT(204, "请求成功，无返回内容"),

    /**
     * 205 Reset Content - 重置内容
     * 服务器成功处理了请求，但不返回任何内容，并要求请求者重置文档视图
     */
    RESET_CONTENT(205, "请求成功，请重置文档视图"),

    /**
     * 206 Partial Content - 部分内容
     * 服务器成功处理了部分GET请求
     */
    PARTIAL_CONTENT(206, "部分内容"),

    /**
     * 207 Multi-Status - 多状态
     * WebDAV扩展，表示多个资源的状态
     */
    MULTI_STATUS(207, "多状态响应"),

    /**
     * 208 Already Reported - 已报告
     * WebDAV扩展，表示成员已在之前响应中枚举
     */
    ALREADY_REPORTED(208, "已报告"),

    /**
     * 226 IM Used - IM已使用
     * 服务器已完成对资源的请求，响应是对当前实例应用的一个或多个实例操作的结果
     */
    IM_USED(226, "IM已使用"),

    // ==================== HTTP标准重定向状态码 (300-399) ====================
    /**
     * 300 Multiple Choices - 多种选择
     * 请求有多个可能的响应，用户或用户代理可以选择一个
     */
    MULTIPLE_CHOICES(300, "多种选择"),

    /**
     * 301 Moved Permanently - 永久移动
     * 请求的资源已永久移动到新URI
     */
    MOVED_PERMANENTLY(301, "资源已永久移动"),

    /**
     * 302 Found - 临时移动
     * 请求的资源临时位于不同的URI
     */
    FOUND(302, "资源临时移动"),

    /**
     * 303 See Other - 查看其他位置
     * 可以在不同的URI下找到对请求的响应，应使用GET方法检索
     */
    SEE_OTHER(303, "请查看其他位置"),

    /**
     * 304 Not Modified - 未修改
     * 自请求头指定的版本以来，资源未被修改
     */
    NOT_MODIFIED(304, "资源未修改"),

    /**
     * 305 Use Proxy - 使用代理
     * 请求的资源必须通过Location字段给出的代理访问
     */
    USE_PROXY(305, "请使用代理访问"),

    /**
     * 306 (Unused) - 未使用
     * 此状态码不再使用，保留用于历史目的
     */
    // SWITCH_PROXY(306, "切换代理"), // 已废弃，不添加

    /**
     * 307 Temporary Redirect - 临时重定向
     * 请求应使用另一个URI重复，但将来的请求仍应使用原始URI
     */
    TEMPORARY_REDIRECT(307, "临时重定向"),

    /**
     * 308 Permanent Redirect - 永久重定向
     * 请求和所有将来的请求应使用另一个URI重复
     */
    PERMANENT_REDIRECT(308, "永久重定向"),

    // ==================== HTTP标准客户端错误 (400-499) ====================
    /**
     * 400 Bad Request - 请求参数错误
     * 服务器无法或不会处理请求，因为存在明显的客户端错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 401 Unauthorized - 未授权访问
     * 请求需要用户认证，认证失败或未提供认证信息
     */
    UNAUTHORIZED(401, "未授权访问"),

    /**
     * 402 Payment Required - 需要付款
     * 保留用于将来使用
     */
    PAYMENT_REQUIRED(402, "需要付款"),

    /**
     * 403 Forbidden - 禁止访问
     * 服务器理解请求但拒绝执行
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 404 Not Found - 资源不存在
     * 服务器找不到请求的资源
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 405 Method Not Allowed - 请求方法不允许
     * 请求的方法不被目标资源支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    /**
     * 406 Not Acceptable - 不可接受
     * 服务器无法根据Accept头生成可接受的响应
     */
    NOT_ACCEPTABLE(406, "请求的媒体类型不被接受"),

    /**
     * 407 Proxy Authentication Required - 需要代理认证
     * 客户端必须首先使用代理进行身份验证
     */
    PROXY_AUTHENTICATION_REQUIRED(407, "需要代理认证"),

    /**
     * 408 Request Timeout - 请求超时
     * 服务器等待请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 409 Conflict - 冲突
     * 请求与资源的当前状态冲突
     */
    CONFLICT(409, "请求冲突"),

    /**
     * 410 Gone - 资源已永久删除
     * 请求的资源已永久删除，不会再可用
     */
    GONE(410, "资源已永久删除"),

    /**
     * 411 Length Required - 需要Content-Length
     * 服务器拒绝接受没有Content-Length头的请求
     */
    LENGTH_REQUIRED(411, "需要Content-Length头"),

    /**
     * 412 Precondition Failed - 前提条件失败
     * 服务器不满足请求的前提条件
     */
    PRECONDITION_FAILED(412, "前提条件失败"),

    /**
     * 413 Payload Too Large - 请求实体过大
     * 请求实体大于服务器允许的大小
     */
    PAYLOAD_TOO_LARGE(413, "请求实体过大"),

    /**
     * 414 URI Too Long - URI过长
     * URI太长，服务器无法处理
     */
    URI_TOO_LONG(414, "URI过长"),

    /**
     * 415 Unsupported Media Type - 不支持的媒体类型
     * 请求的媒体类型不被服务器或资源支持
     */
    UNSUPPORTED_MEDIA_TYPE(415, "请求的媒体类型不被支持"),

    /**
     * 416 Range Not Satisfiable - 范围请求无法满足
     * 客户端请求的范围无法满足
     */
    RANGE_NOT_SATISFIABLE(416, "范围请求无法满足"),

    /**
     * 417 Expectation Failed - 期望失败
     * 服务器无法满足Expect请求头的要求
     */
    EXPECTATION_FAILED(417, "期望失败"),

    /**
     * 422 Unprocessable Entity - 无法处理的实体
     * 请求格式正确，但语义错误，无法处理
     */
    UNPROCESSABLE_ENTITY(422, "请求参数验证失败"),

    /**
     * 423 Locked - 资源已锁定
     * 资源已锁定
     */
    LOCKED(423, "资源已锁定"),

    /**
     * 424 Failed Dependency - 依赖失败
     * 由于之前的请求失败，当前请求无法完成
     */
    FAILED_DEPENDENCY(424, "依赖失败"),

    /**
     * 425 Too Early - 请求过早
     * 服务器不愿意处理可能被重放的请求
     */
    TOO_EARLY(425, "请求过早"),

    /**
     * 426 Upgrade Required - 需要升级
     * 服务器拒绝使用当前协议执行请求
     */
    UPGRADE_REQUIRED(426, "需要升级协议"),

    /**
     * 428 Precondition Required - 需要前提条件
     * 原始服务器要求请求是有条件的
     */
    PRECONDITION_REQUIRED(428, "需要前提条件"),

    /**
     * 429 Too Many Requests - 请求过于频繁
     * 用户在给定时间内发送了太多请求
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    /**
     * 431 Request Header Fields Too Large - 请求头字段过大
     * 服务器不愿意处理请求，因为请求头字段太大
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "请求头字段过大"),

    /**
     * 451 Unavailable For Legal Reasons - 因法律原因不可用
     * 服务器由于法律原因无法提供资源
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "因法律原因不可用"),

    // ==================== HTTP标准服务器错误 (500-599) ====================
    /**
     * 500 Internal Server Error - 服务器内部错误
     * 服务器遇到意外情况，无法完成请求
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 501 Not Implemented - 未实现
     * 服务器不支持请求的功能
     */
    NOT_IMPLEMENTED(501, "功能未实现"),

    /**
     * 502 Bad Gateway - 网关错误
     * 服务器作为网关或代理，从上游服务器收到无效响应
     */
    BAD_GATEWAY(502, "网关错误"),

    /**
     * 503 Service Unavailable - 服务不可用
     * 服务器暂时无法处理请求（过载或维护）
     */
    SERVICE_UNAVAILABLE(503, "服务不可用，请稍后再试"),

    /**
     * 504 Gateway Timeout - 网关超时
     * 服务器作为网关或代理，未及时从上游服务器收到响应
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    /**
     * 505 HTTP Version Not Supported - HTTP版本不支持
     * 服务器不支持请求中使用的HTTP协议版本
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP版本不支持"),

    /**
     * 506 Variant Also Negotiates - 变体也协商
     * 服务器存在内部配置错误
     */
    VARIANT_ALSO_NEGOTIATES(506, "变体也协商"),

    /**
     * 507 Insufficient Storage - 存储空间不足
     * 服务器无法存储完成请求所需的表示
     */
    INSUFFICIENT_STORAGE(507, "存储空间不足"),

    /**
     * 508 Loop Detected - 检测到循环
     * 服务器在处理请求时检测到无限循环
     */
    LOOP_DETECTED(508, "检测到循环"),

    /**
     * 510 Not Extended - 未扩展
     * 服务器需要进一步扩展请求才能完成
     */
    NOT_EXTENDED(510, "未扩展"),

    /**
     * 511 Network Authentication Required - 需要网络认证
     * 客户端需要进行身份验证才能获得网络访问权限
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, "需要网络认证"),

    // ==================== 业务错误 (800-899) ====================
    /**
     * 业务逻辑错误
     */
    BUSINESS_ERROR(800, "业务逻辑错误"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(801, "数据不存在"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(802, "数据已存在"),

    /**
     * 数据状态错误
     */
    DATA_STATUS_ERROR(803, "数据状态错误"),

    /**
     * 操作不允许
     */
    OPERATION_NOT_ALLOWED(804, "操作不允许"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(805, "权限不足"),

    /**
     * 资源已被占用
     */
    RESOURCE_LOCKED(806, "资源已被占用"),

    /**
     * 操作超时
     */
    OPERATION_TIMEOUT(807, "操作超时"),

    /**
     * 数据格式错误
     */
    DATA_FORMAT_ERROR(808, "数据格式错误"),

    /**
     * 数据完整性校验失败
     */
    DATA_INTEGRITY_ERROR(809, "数据完整性校验失败"),

    /**
     * 数据版本冲突
     */
    DATA_VERSION_CONFLICT(810, "数据版本冲突"),

    /**
     * 数据关联错误
     */
    DATA_RELATION_ERROR(811, "数据关联错误"),

    /**
     * 数据范围错误
     */
    DATA_RANGE_ERROR(812, "数据范围错误"),

    /**
     * 数据约束违反
     */
    DATA_CONSTRAINT_VIOLATION(813, "数据约束违反"),

    // ==================== 数据库相关错误 (814-849) ====================
    /**
     * 数据库连接失败
     */
    DATABASE_CONNECTION_FAILED(814, "数据库连接失败"),

    /**
     * 数据库操作失败
     */
    DATABASE_OPERATION_FAILED(815, "数据库操作失败"),

    /**
     * 数据库查询失败
     */
    DATABASE_QUERY_FAILED(816, "数据库查询失败"),

    /**
     * 数据库更新失败
     */
    DATABASE_UPDATE_FAILED(817, "数据库更新失败"),

    /**
     * 数据库插入失败
     */
    DATABASE_INSERT_FAILED(818, "数据库插入失败"),

    /**
     * 数据库删除失败
     */
    DATABASE_DELETE_FAILED(819, "数据库删除失败"),

    /**
     * 主键冲突（唯一键冲突）
     */
    DATABASE_DUPLICATE_KEY(820, "数据库中已存在该记录，请勿重复添加"),

    /**
     * 外键约束违反
     */
    DATABASE_FOREIGN_KEY_VIOLATION(821, "外键约束违反，关联数据不存在"),

    /**
     * 非空约束违反
     */
    DATABASE_NOT_NULL_VIOLATION(822, "非空字段不能为空"),

    /**
     * 唯一约束违反
     */
    DATABASE_UNIQUE_CONSTRAINT_VIOLATION(823, "唯一约束违反，该值已存在"),

    /**
     * 检查约束违反
     */
    DATABASE_CHECK_CONSTRAINT_VIOLATION(824, "数据不符合检查约束条件"),

    /**
     * 数据库事务失败
     */
    DATABASE_TRANSACTION_FAILED(825, "数据库事务执行失败"),

    /**
     * 数据库死锁
     */
    DATABASE_DEADLOCK(826, "数据库死锁，请稍后重试"),

    /**
     * 数据库超时
     */
    DATABASE_TIMEOUT(827, "数据库操作超时，请稍后重试"),

    /**
     * 数据库表不存在
     */
    DATABASE_TABLE_NOT_FOUND(828, "数据库表不存在"),

    /**
     * 数据库字段不存在
     */
    DATABASE_COLUMN_NOT_FOUND(829, "数据库字段不存在"),

    /**
     * 数据库索引不存在
     */
    DATABASE_INDEX_NOT_FOUND(830, "数据库索引不存在"),

    /**
     * SQL语法错误
     */
    DATABASE_SQL_SYNTAX_ERROR(831, "SQL语法错误"),

    /**
     * 数据库权限不足
     */
    DATABASE_PERMISSION_DENIED(832, "数据库权限不足"),

    /**
     * 数据库连接池耗尽
     */
    DATABASE_CONNECTION_POOL_EXHAUSTED(833, "数据库连接池已耗尽，请稍后重试"),

    /**
     * 数据库只读模式
     */
    DATABASE_READ_ONLY(834, "数据库处于只读模式，无法执行写操作"),

    /**
     * 数据库版本不兼容
     */
    DATABASE_VERSION_INCOMPATIBLE(835, "数据库版本不兼容"),

    /**
     * 数据库备份失败
     */
    DATABASE_BACKUP_FAILED(836, "数据库备份失败"),

    /**
     * 数据库恢复失败
     */
    DATABASE_RESTORE_FAILED(837, "数据库恢复失败"),

    // ==================== 认证授权错误 (900-999) ====================
    /**
     * 用户未登录
     */
    USER_NOT_LOGIN(900, "用户未登录"),

    /**
     * 登录已过期
     */
    LOGIN_EXPIRED(901, "登录已过期，请重新登录"),

    /**
     * 用户名或密码错误
     */
    LOGIN_FAILED(902, "用户名或密码错误"),

    /**
     * 账号已被禁用
     */
    ACCOUNT_DISABLED(903, "账号已被禁用"),

    /**
     * 账号已被锁定
     */
    ACCOUNT_LOCKED(904, "账号已被锁定"),

    /**
     * 令牌无效
     */
    TOKEN_INVALID(905, "令牌无效"),

    /**
     * 令牌已过期
     */
    TOKEN_EXPIRED(906, "令牌已过期"),

    /**
     * 刷新令牌无效
     */
    REFRESH_TOKEN_INVALID(907, "刷新令牌无效"),

    /**
     * 刷新令牌已过期
     */
    REFRESH_TOKEN_EXPIRED(908, "刷新令牌已过期"),

    /**
     * 验证码错误
     */
    CAPTCHA_ERROR(909, "验证码错误"),

    /**
     * 验证码已过期
     */
    CAPTCHA_EXPIRED(910, "验证码已过期"),

    /**
     * 第三方登录失败
     */
    THIRD_PARTY_LOGIN_FAILED(911, "第三方登录失败"),

    /**
     * 会话已过期
     */
    SESSION_EXPIRED(912, "会话已过期"),

    /**
     * 会话无效
     */
    SESSION_INVALID(913, "会话无效"),

    /**
     * 密码强度不足
     */
    PASSWORD_WEAK(914, "密码强度不足"),

    /**
     * 密码已过期
     */
    PASSWORD_EXPIRED(915, "密码已过期"),

    /**
     * 账号未激活
     */
    ACCOUNT_NOT_ACTIVATED(916, "账号未激活"),

    /**
     * 账号已注销
     */
    ACCOUNT_CANCELLED(917, "账号已注销"),

    /**
     * 访问频率限制
     */
    ACCESS_RATE_LIMIT(918, "访问频率限制"),

    /**
     * IP地址被禁止
     */
    IP_FORBIDDEN(919, "IP地址被禁止"),

    // ==================== RSA加密相关错误 (1000-1099) ====================
    /**
     * RSA密钥对不存在或已过期
     * 客户端操作：刷新服务端RSA公钥（调用获取服务端公钥接口）
     */
    RSA_KEY_PAIR_NOT_FOUND(1000, "RSA密钥对不存在或已过期，请重新获取服务端公钥"),

    /**
     * RSA密钥对即将过期
     * 客户端操作：刷新服务端RSA公钥（调用获取服务端公钥接口）
     */
    RSA_KEY_PAIR_EXPIRING_SOON(1001, "RSA密钥对即将过期，请重新获取服务端公钥"),

    /**
     * RSA私钥不存在
     * 客户端操作：刷新服务端RSA公钥（调用获取服务端公钥接口）
     */
    RSA_PRIVATE_KEY_NOT_FOUND(1002, "RSA私钥不存在，请重新获取服务端公钥"),

    /**
     * RSA解密失败
     * 客户端操作：刷新服务端RSA公钥（调用获取服务端公钥接口）
     */
    RSA_DECRYPT_FAILED(1003, "RSA解密失败，请重新获取服务端公钥"),

    /**
     * RSA加密失败
     * 客户端操作：检查客户端公钥是否已上传
     */
    RSA_ENCRYPT_FAILED(1004, "RSA加密失败"),

    /**
     * 客户端公钥不存在或已过期
     * 客户端操作：上传客户端RSA公钥（调用上传客户端公钥接口）
     */
    RSA_CLIENT_PUBLIC_KEY_NOT_FOUND(1005, "客户端公钥不存在或已过期，请重新上传客户端公钥"),

    /**
     * 客户端公钥即将过期
     * 客户端操作：上传客户端RSA公钥（调用上传客户端公钥接口）
     */
    RSA_CLIENT_PUBLIC_KEY_EXPIRING_SOON(1006, "客户端公钥即将过期，请重新上传客户端公钥"),

    /**
     * RSA密钥格式错误
     * 客户端操作：检查密钥格式，重新生成或上传
     */
    RSA_KEY_FORMAT_ERROR(1007, "RSA密钥格式错误"),

    /**
     * RSA密钥长度不符合要求
     * 客户端操作：使用符合要求的密钥长度重新生成
     */
    RSA_KEY_LENGTH_ERROR(1008, "RSA密钥长度不符合要求"),

    /**
     * UUID不能为空
     * 客户端操作：确保请求中包含有效的UUID
     */
    RSA_UUID_REQUIRED(1009, "UUID不能为空"),

    /**
     * UUID无效或格式错误
     * 客户端操作：检查UUID格式，使用有效的UUID
     */
    RSA_UUID_INVALID(1010, "UUID无效或格式错误"),

    /**
     * RSA密钥生成失败
     * 客户端操作：稍后重试
     */
    RSA_KEY_GENERATE_FAILED(1011, "RSA密钥生成失败，请稍后重试"),

    /**
     * RSA密钥对不匹配
     * 客户端操作：重新获取服务端公钥
     */
    RSA_KEY_PAIR_MISMATCH(1012, "RSA密钥对不匹配，请重新获取服务端公钥"),

    /**
     * RSA公钥解析失败
     * 客户端操作：检查公钥格式
     */
    RSA_PUBLIC_KEY_PARSE_ERROR(1013, "RSA公钥解析失败"),

    /**
     * RSA私钥解析失败
     * 客户端操作：重新获取服务端公钥
     */
    RSA_PRIVATE_KEY_PARSE_ERROR(1014, "RSA私钥解析失败，请重新获取服务端公钥"),

    // ==================== AES加密相关错误 (1100-1199) ====================
    /**
     * AES密钥不存在或已过期
     * 客户端操作：获取AES加密密钥（调用获取AES密钥接口）
     */
    AES_KEY_NOT_FOUND(1100, "AES密钥不存在或已过期，请重新获取AES密钥"),

    /**
     * AES密钥即将过期
     * 客户端操作：获取AES加密密钥（调用获取AES密钥接口）
     */
    AES_KEY_EXPIRING_SOON(1101, "AES密钥即将过期，请重新获取AES密钥"),

    /**
     * AES密钥已过期
     * 客户端操作：获取AES加密密钥（调用获取AES密钥接口）
     */
    AES_KEY_EXPIRED(1102, "AES密钥已过期，请重新获取AES密钥"),

    /**
     * AES解密失败
     * 客户端操作：获取AES加密密钥（调用获取AES密钥接口）
     */
    AES_DECRYPT_FAILED(1103, "AES解密失败，请重新获取AES密钥"),

    /**
     * AES加密失败
     * 客户端操作：获取AES加密密钥（调用获取AES密钥接口）
     */
    AES_ENCRYPT_FAILED(1104, "AES加密失败，请重新获取AES密钥"),

    /**
     * AES密钥格式错误
     * 客户端操作：重新获取AES密钥
     */
    AES_KEY_FORMAT_ERROR(1105, "AES密钥格式错误，请重新获取AES密钥"),

    /**
     * AES向量格式错误
     * 客户端操作：重新获取AES密钥
     */
    AES_VECTOR_FORMAT_ERROR(1106, "AES向量格式错误，请重新获取AES密钥"),

    /**
     * AES向量长度错误
     * 客户端操作：重新获取AES密钥
     */
    AES_VECTOR_LENGTH_ERROR(1107, "AES向量长度错误，请重新获取AES密钥"),

    /**
     * AES密钥生成失败
     * 客户端操作：稍后重试获取AES密钥
     */
    AES_KEY_GENERATE_FAILED(1108, "AES密钥生成失败，请稍后重试"),

    /**
     * 加密数据为空
     * 客户端操作：检查请求数据
     */
    AES_ENCRYPTED_DATA_EMPTY(1109, "加密数据为空"),

    /**
     * 解密数据为空
     * 客户端操作：检查请求数据
     */
    AES_DECRYPTED_DATA_EMPTY(1110, "解密数据为空"),

    /**
     * AES密钥长度错误
     * 客户端操作：重新获取AES密钥
     */
    AES_KEY_LENGTH_ERROR(1111, "AES密钥长度错误，请重新获取AES密钥"),

    /**
     * AES加密模式不支持
     * 客户端操作：检查加密模式
     */
    AES_MODE_NOT_SUPPORTED(1112, "AES加密模式不支持"),

    /**
     * AES填充模式不支持
     * 客户端操作：检查填充模式
     */
    AES_PADDING_NOT_SUPPORTED(1113, "AES填充模式不支持"),

    // ==================== 系统错误 (100000+) ====================
    /**
     * 未知错误
     */
    UNKNOWN_ERROR(100000, "未知错误"),

    /**
     * 系统维护中
     */
    SYSTEM_MAINTENANCE(100001, "系统维护中，请稍后再试"),

    /**
     * 版本不兼容
     */
    VERSION_INCOMPATIBLE(100002, "版本不兼容，请更新客户端"),

    /**
     * 功能暂未开放
     */
    FEATURE_NOT_AVAILABLE(100003, "功能暂未开放");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String msg;

    /**
     * 客户端操作类型
     * 用于指导客户端在收到错误后应该执行的操作
     */
    public enum Action {
        /**
         * 无需操作
         */
        NONE,

        /**
         * 刷新服务端RSA公钥
         */
        REFRESH_SERVER_PUBLIC_KEY,

        /**
         * 上传客户端RSA公钥
         */
        UPLOAD_CLIENT_PUBLIC_KEY,

        /**
         * 获取AES加密密钥
         */
        GET_AES_KEY,

        /**
         * 重新登录
         */
        RE_LOGIN,

        /**
         * 更新客户端版本
         */
        UPDATE_CLIENT_VERSION,

        /**
         * 稍后重试
         */
        RETRY_LATER
    }

    /**
     * 获取客户端应该执行的操作
     *
     * @return 客户端操作类型
     */
    public Action getAction() {
        // RSA相关错误 (1000-1099)
        if (code >= 1000 && code <= 1099) {
            if (code == RSA_CLIENT_PUBLIC_KEY_NOT_FOUND.getCode()
                    || code == RSA_CLIENT_PUBLIC_KEY_EXPIRING_SOON.getCode()) {
                return Action.UPLOAD_CLIENT_PUBLIC_KEY;
            }
            if (code == RSA_KEY_PAIR_NOT_FOUND.getCode()
                    || code == RSA_KEY_PAIR_EXPIRING_SOON.getCode()
                    || code == RSA_PRIVATE_KEY_NOT_FOUND.getCode()
                    || code == RSA_DECRYPT_FAILED.getCode()
                    || code == RSA_KEY_PAIR_MISMATCH.getCode()
                    || code == RSA_PRIVATE_KEY_PARSE_ERROR.getCode()) {
                return Action.REFRESH_SERVER_PUBLIC_KEY;
            }
        }

        // AES相关错误 (1100-1199)
        if (code >= 1100 && code <= 1199) {
            return Action.GET_AES_KEY;
        }

        // 认证授权错误 (900-999)
        if (code >= 900 && code <= 999) {
            if (code == LOGIN_EXPIRED.getCode()
                    || code == TOKEN_EXPIRED.getCode()
                    || code == REFRESH_TOKEN_EXPIRED.getCode()
                    || code == SESSION_EXPIRED.getCode()) {
                return Action.RE_LOGIN;
            }
        }

        // 版本不兼容
        if (code == VERSION_INCOMPATIBLE.getCode()) {
            return Action.UPDATE_CLIENT_VERSION;
        }

        // 系统维护或服务不可用
        if (code == SYSTEM_MAINTENANCE.getCode() || code == SERVICE_UNAVAILABLE.getCode()) {
            return Action.RETRY_LATER;
        }

        return Action.NONE;
    }

    /**
     * 根据错误码查找错误枚举
     *
     * @param code 错误码
     * @return 错误枚举，如果未找到返回UNKNOWN_ERROR
     */
    public static Error findByCode(int code) {
        for (Error error : values()) {
            if (error.getCode() == code) {
                return error;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 判断是否为成功状态码
     *
     * @return true-是成功状态码（0或200-299），false-不是
     */
    public boolean isSuccess() {
        return code == 0 || (code >= 200 && code <= 299);
    }

    /**
     * 判断是否为信息性状态码
     *
     * @return true-是信息性状态码（100-199），false-不是
     */
    public boolean isInformational() {
        return code >= 100 && code <= 199;
    }

    /**
     * 判断是否为重定向状态码
     *
     * @return true-是重定向状态码（300-399），false-不是
     */
    public boolean isRedirection() {
        return code >= 300 && code <= 399;
    }

    /**
     * 判断是否为HTTP标准状态码（包括所有HTTP状态码）
     *
     * @return true-是HTTP标准状态码（100-599），false-不是
     */
    public boolean isHttpStatusCode() {
        return (code >= 100 && code <= 599);
    }

    /**
     * 判断是否为加密相关错误
     *
     * @return true-是加密相关错误，false-不是
     */
    public boolean isEncryptionError() {
        return (code >= 1000 && code <= 1199);
    }

    /**
     * 判断是否为RSA加密错误
     *
     * @return true-是RSA加密错误，false-不是
     */
    public boolean isRsaError() {
        return (code >= 1000 && code <= 1099);
    }

    /**
     * 判断是否为AES加密错误
     *
     * @return true-是AES加密错误，false-不是
     */
    public boolean isAesError() {
        return (code >= 1100 && code <= 1199);
    }

    /**
     * 构造函数
     *
     * @param code 错误码
     * @param msg  错误信息
     */
    Error(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
