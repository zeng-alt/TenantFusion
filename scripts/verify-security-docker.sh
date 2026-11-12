#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
pg_container="tenantfusion-security-pg-$$"
redis_container="tenantfusion-security-redis-$$"
cleanup() {
    docker rm -f "$pg_container" "$redis_container" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

# 随机本机端口、临时文件系统、不挂载现有数据库或 Redis 卷。
docker run -d --name "$pg_container" \
    -e POSTGRES_DB=security_test -e POSTGRES_PASSWORD=test-only \
    -p 127.0.0.1::5432 --tmpfs /var/lib/postgresql/data postgres:16-alpine >/dev/null
docker run -d --name "$redis_container" -p 127.0.0.1::6379 redis:7-alpine >/dev/null

ready=false
for attempt in {1..30}; do
    if docker exec "$pg_container" pg_isready -h 127.0.0.1 -U postgres >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
if [[ "$ready" != true ]]; then
    docker logs "$pg_container"
    exit 1
fi
docker exec "$pg_container" createdb -U postgres security_tenants

export SECURITY_DOCKER_TESTS=true
export SECURITY_TEST_PG_PORT
export SECURITY_TEST_REDIS_PORT
SECURITY_TEST_PG_PORT="$(docker inspect -f '{{(index (index .NetworkSettings.Ports "5432/tcp") 0).HostPort}}' "$pg_container")"
SECURITY_TEST_REDIS_PORT="$(docker inspect -f '{{(index (index .NetworkSettings.Ports "6379/tcp") 0).HostPort}}' "$redis_container")"
cd "$repo_root"
./gradlew :backend:admin:test --tests '*AdminSecurityDockerTest' "$@"
