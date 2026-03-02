var vm = new Vue({
    el: '#rrapp',
    data: {
        checklistStorageKey: 'autumn.docs.onboarding.checklist.v1',
        moduleProgressStorageKey: 'autumn.docs.module.progress.v1',
        learningPaths: [
            {
                title: '入门路径（Day 1）',
                label: '推荐新同学',
                steps: [
                    { name: '环境与启动', page: 'quickstart' },
                    { name: '架构与模块分层', page: 'architecture' },
                    { name: 'AI 协作开发规范', page: 'ai-collab' },
                    { name: 'Handler 机制与扩展点', page: 'handler' },
                    { name: '系统管理基础（用户/角色/菜单）', page: 'sys' }
                ]
            },
            {
                title: '进阶路径（Day 2-3）',
                label: '推荐开发同学',
                steps: [
                    { name: '代码生成能力', page: 'gen' },
                    { name: '定时任务与日志', page: 'job' },
                    { name: '认证授权与加密握手', page: 'oauth' },
                    { name: '混合加解密（RSA+AES）', page: 'hybrid-crypto' }
                ]
            },
            {
                title: '运维与安全（Week 1）',
                label: '推荐运维/架构同学',
                steps: [
                    { name: '缓存体系与失效同步', page: 'cache' },
                    { name: '队列体系与消费治理', page: 'queue' },
                    { name: '数据库备份与恢复', page: 'db' },
                    { name: '防火墙策略与黑白名单', page: 'wall' },
                    { name: '文件存储与配置治理', page: 'oss' }
                ]
            }
        ],
        onboardingChecklist: [
            { item: '完成本地部署并成功登录后台', page: 'quickstart', done: false },
            { item: '可解释核心模块边界与请求链路', page: 'architecture', done: false },
            { item: '掌握多项目 AI 协作提示模板并完成一次调用', page: 'ai-collab', done: false },
            { item: '可说明 Handler 在模块与框架间的调用关系', page: 'handler', done: false },
            { item: '完成一次用户与角色授权演示', page: 'sys', done: false },
            { item: '完成一次缓存查询与清理演示', page: 'cache', done: false },
            { item: '完成一次队列创建、投递与消费演示', page: 'queue', done: false },
            { item: '完成一次表到代码生成演示', page: 'gen', done: false },
            { item: '完成一次定时任务创建与执行', page: 'job', done: false },
            { item: '完成一次防火墙黑名单演示', page: 'wall', done: false }
        ],
        moduleProgress: [
            { key: 'ai-collab', title: 'AI 协作开发', page: 'ai-collab', done: false },
            { key: 'handler', title: 'Handler 机制', page: 'handler', done: false },
            { key: 'sys', title: '系统管理', page: 'sys', done: false },
            { key: 'cache', title: '缓存体系', page: 'cache', done: false },
            { key: 'queue', title: '队列体系', page: 'queue', done: false },
            { key: 'gen', title: '代码生成', page: 'gen', done: false },
            { key: 'job', title: '定时任务', page: 'job', done: false },
            { key: 'db', title: '数据库运维', page: 'db', done: false },
            { key: 'oauth', title: '认证授权', page: 'oauth', done: false },
            { key: 'usr', title: '用户域能力', page: 'usr', done: false },
            { key: 'oss', title: '文件存储', page: 'oss', done: false },
            { key: 'lan', title: '多语言', page: 'lan', done: false },
            { key: 'spm', title: '超级位置模型', page: 'spm', done: false },
            { key: 'wall', title: '防火墙', page: 'wall', done: false }
        ],
        features: [
            {
                key: 'ai-collab',
                title: 'AI 协作开发（多项目）',
                status: 'stable',
                detailPage: 'ai-collab',
                items: [
                    '多项目上下文喂给策略（框架层/项目层/任务层）',
                    '统一提示词模板（新增功能/修复缺陷）',
                    '框架能力复用硬约束（缓存/队列/加密）',
                    'AGENTS.md 在业务项目的落地建议'
                ],
                todo: [
                    '补充不同团队角色的提示词变体（后端/前端/测试）',
                    '补充真实案例：从需求到代码交付的完整 AI 会话'
                ]
            },
            {
                key: 'handler',
                title: 'Handler 机制（autumn-handler）',
                status: 'stable',
                detailPage: 'handler',
                items: [
                    '按接口解耦模块实现与框架主流程',
                    '通过 ConditionalOnMissingBean 提供默认兜底',
                    '通过 Order 控制多实现优先级',
                    '支持页面、过滤链、插件、集群映射等扩展'
                ],
                todo: [
                    '补充各 Handler 在主流程中的时序图',
                    '补充典型模块接入代码片段'
                ]
            },
            {
                key: 'sys',
                title: '系统管理（sys）',
                status: 'stable',
                detailPage: 'sys',
                items: [
                    '用户、角色、部门、菜单与权限管理',
                    '字典、分类、配置参数管理',
                    '系统日志与审计能力',
                    '插件控制与基础页面调度'
                ],
                todo: [
                    '补充权限模型图（角色-菜单-数据范围）',
                    '补充管理员常用操作手册',
                    '补充系统配置项清单'
                ]
            },
            {
                key: 'gen',
                title: '代码生成（gen）',
                status: 'stable',
                detailPage: 'gen',
                items: [
                    '按数据表生成基础代码结构',
                    '模板化代码生成（.vm）',
                    '生成后支持快速 CRUD 开发'
                ],
                todo: [
                    '补充“从建表到生成代码”的端到端示例',
                    '补充模板扩展规范与命名约定'
                ]
            },
            {
                key: 'cache',
                title: '缓存体系（cache）',
                status: 'stable',
                detailPage: 'cache',
                items: [
                    '两级缓存（EhCache + Redis）读写流程',
                    'Redis Pub/Sub 失效广播与跨实例一致性',
                    '共享缓存抽象（ShareCacheService）与业务继承用法',
                    '系统管理员缓存运维接口（查询/搜索/删除/清空）'
                ],
                todo: [
                    '补充缓存容量规划建议与命名规范',
                    '补充高并发热点 key 保护策略'
                ]
            },
            {
                key: 'job',
                title: '定时任务（job）',
                status: 'stable',
                detailPage: 'job',
                items: [
                    '接口式定时任务（LoopJob）优先，减少 cron 表达式错误',
                    '任务管理、任务分配与执行日志统一管理',
                    '全局/分类/单任务启停、触发、统计与告警',
                    '连续失败自动禁用、超时与跳过率观测'
                ],
                todo: [
                    '补充停服恢复后的补偿执行策略',
                    '补充大规模任务并行执行压测建议'
                ]
            },
            {
                key: 'queue',
                title: '队列体系（queue）',
                status: 'iterating',
                detailPage: 'queue',
                items: [
                    '多类型队列支持（Memory/Redis List/Redis Stream/Delay/Priority）',
                    '多发送模式（普通/延迟/定时/优先级/批量）',
                    '自动消费者启停与空闲超时治理',
                    '历史消息、死信重试、消息搬迁运维能力'
                ],
                todo: [
                    '补充典型业务的幂等消费模板',
                    '补充消费者并发与容量压测建议'
                ]
            },
            {
                key: 'db',
                title: '数据库运维（db）',
                status: 'iterating',
                detailPage: 'db',
                items: [
                    '数据库备份与备份策略管理',
                    '备份上传与恢复入口',
                    '数据库相关管理页面'
                ],
                todo: [
                    '补充恢复流程演练文档',
                    '补充备份安全与权限边界说明'
                ]
            },
            {
                key: 'oauth_client',
                title: '认证授权（oauth / client）',
                status: 'iterating',
                detailPage: 'oauth',
                items: [
                    'OAuth2 客户端管理（ClientDetails）',
                    'Token 存储管理（TokenStore）',
                    '加密密钥与 RSA 支持',
                    'Web 认证组合能力'
                ],
                todo: [
                    '补充认证流程时序图',
                    '补充典型接入配置示例'
                ]
            },
            {
                key: 'usr',
                title: '用户域能力（usr）',
                status: 'stable',
                detailPage: 'usr',
                items: [
                    '用户开放能力管理（UserOpen）',
                    '用户令牌管理（UserToken）',
                    '登录日志与用户资料维护'
                ],
                todo: [
                    '补充用户安全策略与Token策略',
                    '补充开放能力接入示例'
                ]
            },
            {
                key: 'oss',
                title: '文件存储（oss）',
                status: 'stable',
                detailPage: 'oss',
                items: [
                    '对象存储配置与上传管理',
                    '文件访问与存储策略支持',
                    '云存储扩展基础能力'
                ],
                todo: [
                    '补充不同存储后端对比表',
                    '补充文件生命周期策略示例'
                ]
            },
            {
                key: 'lan',
                title: '多语言（lan）',
                status: 'stable',
                detailPage: 'lan',
                items: [
                    '语言资源管理',
                    '支持语言列表维护',
                    '系统多语言切换能力'
                ],
                todo: [
                    '补充新增语言标准流程',
                    '补充翻译键命名规范'
                ]
            },
            {
                key: 'spm',
                title: '超级位置模型（spm）',
                status: 'iterating',
                detailPage: 'spm',
                items: [
                    '超级位置模型管理',
                    '访问日志记录与统计入口',
                    '埋点类能力支持'
                ],
                todo: [
                    '补充埋点字段定义规范',
                    '补充统计报表说明'
                ]
            },
            {
                key: 'wall',
                title: '防火墙（wall）',
                status: 'stable',
                detailPage: 'wall',
                items: [
                    'IP 黑名单/白名单管理',
                    'URL 黑名单与主机访问控制',
                    '访问统计与跳转防护',
                    '防护总开关与策略控制'
                ],
                todo: [
                    '补充策略优先级说明',
                    '补充误拦截排查手册'
                ]
            },
            {
                key: 'future',
                title: '新功能规划区',
                status: 'planned',
                detailPage: 'roadmap',
                items: [
                    '功能需求背景与目标',
                    '技术方案与影响评估',
                    '灰度计划与发布计划'
                ],
                todo: [
                    '为每个新功能增加验收标准（DoD）',
                    '为每个新功能增加回滚方案'
                ]
            }
        ]
    },
    computed: {
        completedCount: function () {
            return this.onboardingChecklist.filter(function (x) {
                return x.done;
            }).length;
        },
        completionRate: function () {
            if (!this.onboardingChecklist.length) {
                return 0;
            }
            return Math.round((this.completedCount * 100) / this.onboardingChecklist.length);
        },
        moduleCompletedCount: function () {
            return this.moduleProgress.filter(function (x) {
                return x.done;
            }).length;
        },
        moduleCompletionRate: function () {
            if (!this.moduleProgress.length) {
                return 0;
            }
            return Math.round((this.moduleCompletedCount * 100) / this.moduleProgress.length);
        }
    },
    methods: {
        parseQuery: function () {
            var query = window.location.search || '';
            var params = {};
            if (!query || query.length < 2) {
                return params;
            }
            var parts = query.substring(1).split('&');
            for (var i = 0; i < parts.length; i++) {
                var seg = parts[i].split('=');
                var k = decodeURIComponent(seg[0] || '').trim();
                var v = decodeURIComponent(seg[1] || '').trim();
                if (k) {
                    params[k] = v;
                }
            }
            return params;
        },
        applyDoneFromQuery: function () {
            var params = this.parseQuery();
            var done = params.done;
            if (!done) {
                return;
            }
            // 更新模块进度
            for (var i = 0; i < this.moduleProgress.length; i++) {
                if (this.moduleProgress[i].page === done) {
                    this.moduleProgress[i].done = true;
                }
            }
            // 更新 onboarding 清单
            for (var j = 0; j < this.onboardingChecklist.length; j++) {
                if (this.onboardingChecklist[j].page === done) {
                    this.onboardingChecklist[j].done = true;
                }
            }
            this.saveModuleProgress();
            this.saveChecklistState();
        },
        loadChecklistState: function () {
            try {
                var raw = localStorage.getItem(this.checklistStorageKey);
                if (!raw) {
                    return;
                }
                var saved = JSON.parse(raw);
                if (!Array.isArray(saved)) {
                    return;
                }
                for (var i = 0; i < this.onboardingChecklist.length; i++) {
                    this.onboardingChecklist[i].done = !!saved[i];
                }
            } catch (e) {
            }
        },
        saveChecklistState: function () {
            try {
                var state = this.onboardingChecklist.map(function (x) {
                    return !!x.done;
                });
                localStorage.setItem(this.checklistStorageKey, JSON.stringify(state));
            } catch (e) {
            }
        },
        toggleChecklist: function () {
            this.saveChecklistState();
        },
        resetChecklist: function () {
            for (var i = 0; i < this.onboardingChecklist.length; i++) {
                this.onboardingChecklist[i].done = false;
            }
            this.saveChecklistState();
        },
        loadModuleProgress: function () {
            try {
                var raw = localStorage.getItem(this.moduleProgressStorageKey);
                if (!raw) {
                    return;
                }
                var saved = JSON.parse(raw);
                if (!saved || typeof saved !== 'object') {
                    return;
                }
                for (var i = 0; i < this.moduleProgress.length; i++) {
                    var p = this.moduleProgress[i].page;
                    this.moduleProgress[i].done = !!saved[p];
                }
            } catch (e) {
            }
        },
        saveModuleProgress: function () {
            try {
                var map = {};
                for (var i = 0; i < this.moduleProgress.length; i++) {
                    var p = this.moduleProgress[i];
                    map[p.page] = !!p.done;
                }
                localStorage.setItem(this.moduleProgressStorageKey, JSON.stringify(map));
            } catch (e) {
            }
        },
        toggleModuleProgress: function () {
            this.saveModuleProgress();
        },
        resetModuleProgress: function () {
            for (var i = 0; i < this.moduleProgress.length; i++) {
                this.moduleProgress[i].done = false;
            }
            this.saveModuleProgress();
        },
        isModuleDone: function (page) {
            if (!page) {
                return false;
            }
            for (var i = 0; i < this.moduleProgress.length; i++) {
                if (this.moduleProgress[i].page === page) {
                    return !!this.moduleProgress[i].done;
                }
            }
            return false;
        },
        toggleFeatureDone: function (page) {
            if (!page) {
                return;
            }
            for (var i = 0; i < this.moduleProgress.length; i++) {
                if (this.moduleProgress[i].page === page) {
                    this.moduleProgress[i].done = !this.moduleProgress[i].done;
                    break;
                }
            }
            this.saveModuleProgress();
        }
    },
    mounted: function () {
        this.loadChecklistState();
        this.loadModuleProgress();
        this.applyDoneFromQuery();
    }
});
