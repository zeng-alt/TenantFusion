# 本地 k3s 基座

通过 k3d 在 Docker Desktop 中运行一个 k3s server，版本固定为 `v1.37.2+k3s1`。
保留 k3s 自带的 Traefik、CoreDNS、Metrics Server 和 local-path 存储。
应用命名空间为 `tenantfusion`；现有 IDEA 应用和 Docker PostgreSQL、Redis 继续按原方式运行。

## 创建与连接

在项目根目录执行：

```sh
brew install k3d
k3d cluster create --config infra/k3s/k3d.yaml
kubectl --context k3d-tenantfusion apply -f infra/k3s/namespace.yaml
kubectl --context k3d-tenantfusion get nodes
kubectl --context k3d-tenantfusion get pods -A
```

k3d 会将连接信息合并到 `~/.kube/config`，并切换到 `k3d-tenantfusion` 上下文。
已有的其他集群配置会保留。

| 入口 | 本机地址 |
| --- | --- |
| Kubernetes API | `https://127.0.0.1:16443` |
| Ingress HTTP | `http://127.0.0.1:18080` |
| Ingress HTTPS | `https://127.0.0.1:18443` |

应用 Ingress 使用 `ingressClassName: traefik`，持久卷使用 `storageClassName: local-path`。
`local-path` 数据存放于对应节点，停止、重启集群会保留数据；删除集群会删除节点及其本地数据。

## 停止与恢复

```sh
k3d cluster stop tenantfusion
k3d cluster start tenantfusion
kubectl --context k3d-tenantfusion get nodes
```

## Headlamp 管理台

Headlamp `v0.46.0` 部署在 `headlamp` 命名空间，使用现有 Traefik 入口：

```sh
kubectl --context k3d-tenantfusion apply -f infra/k3s/headlamp.yaml
kubectl --context k3d-tenantfusion -n headlamp rollout status deployment/headlamp
```

访问地址：[http://headlamp.localhost:18080](http://headlamp.localhost:18080)。
使用支持 `.localhost` 本地域名的浏览器访问，无需修改 hosts。

登录时选择集群 `main`（当前 `tenantfusion` 集群），使用 ServiceAccount Token。
`headlamp-admin` 是本地集群管理员账户；Headlamp 服务自身使用独立的 `headlamp` 账户。
执行下列命令，将 8 小时有效的登录 Token 复制到 macOS 剪贴板：

```sh
kubectl --context k3d-tenantfusion -n headlamp create token headlamp-admin --duration=8h | pbcopy
```

在登录页的 Token 输入框粘贴后登录。Token 到期后重新执行上面的命令即可。
初次生成的 Token 也保存在忽略提交的 `build/local-dev/headlamp-login-token`，文件权限为 `0600`。

集群通过 `k3d cluster start tenantfusion` 恢复后，Headlamp 也会随集群恢复，访问无需额外启动 port-forward。

部署与登录方式参考 [Headlamp 官方文档](https://headlamp.dev/docs/latest/installation/)。
