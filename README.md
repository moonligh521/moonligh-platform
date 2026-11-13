# moonligh-platform

公司级 Java 脚手架仓。业务仓只依赖本仓提供的能力，业务仓之间不互为 jar 依赖。

## 架构位置

```text
moonligh-platform          ← 统一返回、错误码、鉴权、Redis、MQ、依赖版本
        ↑ 依赖
registration-assistant     ← 账号 / 患者 / 网关
nutritionist               ← 营养业务（患者只存引用）
clinical-record            ← 临床事实与当前快照
```

现行架构规范见 [`docs/企业架构规范.md`](docs/企业架构规范.md)。

## 模块

| 模块 | 职责 |
| --- | --- |
| `platform-bom` | 依赖版本对齐 |
| `platform-common` | Result、ErrorCode、TraceId、全局异常 |
| `platform-security` | JWT / 账号头鉴权 |
| `platform-redis` | Redis / Redisson |
| `platform-mq` | RocketMQ |
| `platform-job` | XXL-JOB |
| `platform-model` | 跨服务可共享模型 |
| `platform-observability` | 观测 |
| `platform-gateway` | Gateway 横切：限流、在途、Trace、JWT（不含业务路由） |

Maven 坐标：`com.moonligh:platform-*`。Java 包名暂保持 `com.moonligh.consult.*`，与历史代码兼容。

配置前缀：

- `moonligh.gateway`：网关匿名路径、staff 前缀、限流
- `consult.auth`：JWT、内部令牌、AccountAuth 开放/staff 路径



## 错误码号段

- `1xxx` 通用
- `11xx～13xx` 挂号 / 问诊
- `21xx～30xx` 营养
- `31xx` 临床数据仓库
- 新业务先在本仓申请号段，再改代码

## 边界

- 本仓：只放跨业务可复用能力
- 不放：挂号业务、营养业务、患者主数据表
- 挂号仓还剩哪些该迁入：见 [`docs/挂号仓待迁清单.md`](docs/挂号仓待迁清单.md)
- 临床事实放在独立服务 `clinical-record`。平台仓不新增 `platform-his`，也不存放病情模型

## 许可

Apache-2.0
