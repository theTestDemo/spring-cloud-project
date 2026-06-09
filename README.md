# 微服务商城演示项目

基于 Spring Cloud Alibaba 的微服务实战项目，演示了完整的电商核心功能：用户认证、商品管理、购物车、订单、支付、秒杀、**商品评论**等，并集成了服务注册与发现、配置中心、API 网关、熔断降级、分布式锁、消息队列等主流技术。

## 📚 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 8 | 基础运行环境 |
| Spring Boot | 2.7.18 | 快速开发框架 |
| Spring Cloud | 2021.0.9 | 微服务生态 |
| Spring Cloud Alibaba | 2021.0.5.0 | 阿里微服务组件 |
| Nacos | 2.2.3 | 服务注册与配置中心 |
| Sentinel | 1.8.6 | 熔断限流 |
| Gateway | 3.1.9 | API 网关 |
| OpenFeign | 3.1.9 | 声明式远程调用 |
| RocketMQ | 5.0.0 | 消息队列（异步解耦） |
| MyBatis | 3.5.14 | ORM 框架 |
| PageHelper | 1.4.7 | MyBatis 分页插件 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.x / 7.x | 缓存 + 分布式锁 |
| Redisson | 3.17.7 | 分布式锁实现 |
| JWT | 0.11.5 | 身份认证 |
| Lombok | 1.18.30 | 简化代码 |

## 📦 模块结构

cloud-demo/
├── common/ // 通用模块（工具类、自动配置）
├── gateway/ // 网关（统一入口、JWT 校验）
├── user-service/ // 用户服务（注册、登录、JWT）
├── good-service/ // 商品服务（商品查询、库存、**商品评论**）
├── order-service/ // 订单服务（购物车、订单、支付、秒杀）
├── nacos-discovery/ // 服务提供者示例
└── consumer/ // 服务消费者示例（Feign 调用）

text

## 🚀 快速启动

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 8.0
- Redis
- RocketMQ（或使用 Docker）
- Nacos

### 启动步骤

#### 1. 启动 Nacos（单机模式）
```bash
cd D:\nacos\nacos\bin
.\startup.cmd -m standalone
访问控制台：http://localhost:8848/nacos

#### 2. 启动 Redis
bash
# Windows 版直接运行 redis-server.exe
redis-server

#### 3. 启动 RocketMQ
bash
# NameServer
cd D:\rocketmq\bin
.\mqnamesrv.cmd

# Broker（新开窗口）
cd D:\rocketmq\bin
.\mqbroker.cmd -n localhost:9876 -c D:\rocketmq\conf\broker.conf

#### 4. 初始化数据库
数据库表结构请参考各服务模块的 mapper 文件或手动执行 SQL（因篇幅限制不单独提供）

#### 5. 启动微服务（在 IDEA 中依次运行）
good-service（商品服务，端口 8085）
user-service（用户服务，端口 8083）
order-service（订单服务，端口 8084）
gateway（网关，端口 8080）

#### 6. 测试接口（推荐 Postman）
详见下方 接口示例

## 🔥 核心功能亮点
1. 用户认证
   基于 BCrypt 加密存储密码
   JWT 生成 Token，网关统一认证
   请求头传递用户 ID（X-User-Id），防止越权

2. 商品服务
   Redis 缓存商品信息，减轻数据库压力
   库存扣减使用乐观锁（WHERE stock >= #{quantity}）
   记录库存变更日志，便于审计
   商品评论功能（见下方独立条目）

3. 购物车 & 订单
   购物车使用 MySQL 唯一约束实现“有则增加，无则插入”
   下单使用分布式锁（Redisson）防止重复提交
   订单明细批量插入，提升性能
   下单后发送延时消息（30 分钟超时自动取消）

4. 支付
   订单状态乐观锁防止重复支付
   支付成功后记录支付流水，发送支付消息

5. 秒杀（高并发核心）
   秒杀库存预热到 Redis（原子扣减，无超卖）
   分布式锁 + 防重表（唯一索引）防止同一用户重复秒杀
   秒杀成功后发送消息，异步创建正式订单
   秒杀接口单独限流（QPS=5）

6. 可靠性保障
   限流（Sentinel）：对关键接口（下单、秒杀、扣库存）设置 QPS 阈值
   熔断降级（Feign + Sentinel）：服务不可用时返回友好提示
   分布式锁（Redisson）：防止并发下单、并发秒杀
   全局异常处理：统一返回格式，消除 Controller 中的 try-catch

7. 异步线程池与事件驱动
   - 自定义订单通知线程池（orderNotifyExecutor），精细化控制核心线程数、最大线程数、有界队列与 CallerRunsPolicy 拒绝策略
   - 基于 @Async + @TransactionalEventListener 实现事务提交后异步解耦
   - 结合 CompletableFuture 实现多任务并发，不阻塞主流程
   - 拒绝策略触发时自动降级，由提交线程自行执行任务，确保任务不丢失

8. 商品评论（新增功能）
   用户可对已支付订单中的商品进行评分和文字评价
   支持图片上传（逗号分隔多图链接）
   评论后动态更新商品评分和评论数
   查询评论列表支持分页、排序，并关联用户信息
   删除评论使用编程式事务，保证评论状态与商品统计的原子性
   跨服务协作：good-service 通过 Feign 调用 order-service 校验订单状态，调用 user-service 批量获取用户昵称

## 📝 接口示例

### 用户注册
http
POST http://localhost:8083/user/register
Content-Type: application/json

{
    "username": "zhangsan",
    "password": "123456",
    "email": "zhangsan@example.com"
}

### 用户登录
http
POST http://localhost:8083/user/login
Content-Type: application/x-www-form-urlencoded

username=zhangsan&password=123456
响应：{"code":200,"msg":"操作成功","data":"<JWT_TOKEN>"}

### 查询用户信息（需要 Token）
http
GET http://localhost:8080/user/1
Authorization: Bearer <JWT_TOKEN>

### 秒杀（通过网关）
http
GET http://localhost:8080/order/secKill?goodId=1
Authorization: Bearer <JWT_TOKEN>
X-User-Id: 1

### 创建订单（从购物车结算）
http
GET http://localhost:8080/order/buyGoods?userId=1
Authorization: Bearer <JWT_TOKEN>

### 商品评论

#### 添加评论（需要 Token）
http
POST http://localhost:8080/good/addReview
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
    "orderNo": "837357036881711104",
    "productId": 10001,
    "rating": 5,
    "content": "商品很好用",
    "pics": "https://img.example.com/1.jpg,https://img.example.com/2.jpg"
}

#### 查询评论列表（公开）
http
GET http://localhost:8080/good/review/list?productId=10001&page=1&pageSize=10&sort=create_time

#### 删除评论（需要 Token）
http
DELETE http://localhost:8080/good/review/delete?reviewNo=850065875238260736
Authorization: Bearer <JWT_TOKEN>

## 🧪 压测与验证
限流测试（Sentinel）
使用 JMeter 或 Postman Runner 连续快速请求 /good/reduceStock，超出 QPS 阈值后返回 429。

秒杀并发测试
模拟 50 个线程同时秒杀，观察：
Redis 库存原子递减，不超卖
同一用户只能秒杀一次
秒杀成功订单异步创建

## 📌 注意事项
RocketMQ 存储路径建议改为 D 盘，避免 C 盘占满
生产环境 JWT 密钥应从配置中心读取，勿硬编码
限流阈值根据实际压测调整
分布式锁的 watchDog 仅在无参 lock.lock() 时生效，指定 leaseTime 后不会自动续期
评论删除使用软删除（status 标记），不会真删除数据
批量查询用户信息时，good-service 使用 @PostMapping 传参，避免 GET 请求 URL 过长

## 🤝 贡献
本项目为个人学习作品，旨在展示微服务架构能力。欢迎交流探讨。

## 📄 许可证
Apache License 2.0

## 📧 联系方式
作者：胡孟阳
GitHub：[https://github.com/theTestDemo]