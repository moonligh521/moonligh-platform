# 更新日志

## [Unreleased]

### Added

- 从 `registration-assistant/consult-framework` 迁出并建立公司脚手架仓。
- 提供 `platform-bom`、`platform-common`、`platform-security`、`platform-redis`、`platform-mq`、`platform-job`、`platform-model`、`platform-observability`。
- `ErrorCode` 纳入营养服务 21xx～30xx 号段。
- 增加现行企业架构规范。临床数据仓库错误码登记为 `31xx`。MyBatis 版本改由 `platform-bom` 管理。

### Changed

- 文档只保留架构与模块边界说明，不涉及私服或部署流程。
