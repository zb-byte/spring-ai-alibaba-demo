# A2A Demo - 快速开始

## 📋 项目概述
演示 基于Google A2A 协议的三种通信方式：JSONRPC、REST、GRPC。
其中 JSONRPC模块依赖的是spring-ai-alibaba ，一个基于Google A2A 官方 SDK 和 spring-ai 二次开发的项目。
REST 和 GRPC 基于Google A2A 官方 SDK。


## 🚀 快速开始

### 前置条件

1. **JDK 21+**
   ```bash
   java -version  # 确保版本 >= 21
   ```

2. **Maven 3.9+**
   ```bash
   mvn -version
   ```

3. **LLM 服务配置**
   
   项目默认使用以下配置（可通过环境变量覆盖）：
   ```bash
   export DEFAULT_LLM_API_KEY="your-api-key"
   export DEFAULT_LLM_BASE_URL="https://ark.cn-beijing.volces.com"
   export DEFAULT_LLM_MODEL_NAME="deepseek-v3-250324"
   export DEFAULT_LLM_COMPLETIONS_PATH="/api/v3/chat/completions"
   export DEFAULT_LLM_EMBEDDINGS_PATH="/api/v3/embeddings"
   ```

## 📚 传输协议文档

- **jsonrpc 传输协议** - 访问 [a2a-client-module-jsonrpc/README.md](a2a-client-module-jsonrpc/README.md)
- **gRPC 传输协议** - 访问 [a2a-client-module-grpc/README.md](a2a-client-module-grpc/README.md)
- **rest 传输协议** - 访问 [a2a-client-module-rest/README.md](a2a-client-module-rest/README.md)

