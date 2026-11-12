#!/usr/bin/env python3
"""Smoke-test an admin Native executable against disposable PostgreSQL and Redis."""

import argparse
import base64
import json
import http.cookiejar
import os
import pathlib
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request
import urllib.parse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("binary", type=pathlib.Path, help="Path to the admin Native executable")
parser.add_argument("--native-arg", action="append", default=[], help="Additional Native runtime argument")
arguments = parser.parse_args()
binary = arguments.binary.resolve(strict=True)
root = pathlib.Path(__file__).resolve().parents[1]
report = root / "build/reports/boot4-upgrade"
report.mkdir(parents=True, exist_ok=True)
suffix = str(os.getpid())
pg = "tenantfusion-boot4-native-pg-" + suffix
redis = "tenantfusion-boot4-native-redis-" + suffix
process = None
checks = []
cookies = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))


def docker(*args, input=None):
    return subprocess.run(["docker", *args], input=input, text=True, check=True, capture_output=True).stdout.strip()


def sql(statement):
    return docker(
        "exec", "-i", pg, "psql", "-U", "postgres", "-d", "security_test", "-v", "ON_ERROR_STOP=1", input=statement
    )


def request(path, method="GET", body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(address + path, data=payload, headers=headers, method=method)
    try:
        with opener.open(req, timeout=15) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def expect(name, response, code):
    if response[0] != code:
        detail = (
            json.loads(response[1]).get("detail")
            or json.loads(response[1]).get("message")
            or json.loads(response[1]).get("title")
        )
        raise AssertionError(name + " expected " + str(code) + " got " + str(response[0]) + ": " + str(detail))
    checks.append({"check": name, "status": code})
    print(name, code, flush=True)


def login_with_captcha():
    challenge = request("/v1/auth/captcha")
    expect("captcha_png", challenge, 200)
    assert challenge[1].startswith(b"\x89PNG\r\n\x1a\n"), "Captcha response is not PNG"
    key = next(cookie.value for cookie in cookies if cookie.name == "captcha_key")
    stored_key = docker("exec", redis, "redis-cli", "--scan", "--pattern", "*captcha:*" + key)
    code = json.loads(docker("exec", redis, "redis-cli", "--raw", "GET", stored_key))
    return request(
        "/v1/login/jwt?" + urllib.parse.urlencode({"captchaCode": code}),
        "POST",
        {"username": "native-upgrade", "password": "native-test-only", "rememberMe": True},
    )


try:
    docker(
        "run",
        "-d",
        "--name",
        pg,
        "-e",
        "POSTGRES_DB=security_test",
        "-e",
        "POSTGRES_PASSWORD=test-only",
        "-p",
        "127.0.0.1::5432",
        "--tmpfs",
        "/var/lib/postgresql/data",
        "postgres:16-alpine",
    )
    docker("run", "-d", "--name", redis, "-p", "127.0.0.1::6379", "redis:7-alpine")
    for _ in range(30):
        ready = (
            subprocess.run(["docker", "exec", pg, "pg_isready", "-U", "postgres"], capture_output=True).returncode == 0
        )
        if ready:
            break
        time.sleep(1)
    if not ready:
        raise RuntimeError("Isolated PostgreSQL not ready")
    docker("exec", pg, "createdb", "-U", "postgres", "security_tenants")

    def port(name, internal):
        info = json.loads(docker("inspect", name))[0]
        return info["NetworkSettings"]["Ports"][internal][0]["HostPort"]

    pg_port = port(pg, "5432/tcp")
    redis_port = port(redis, "6379/tcp")
    with socket.socket() as available:
        available.bind(("127.0.0.1", 0))
        server_port = available.getsockname()[1]
    address = "http://127.0.0.1:" + str(server_port)
    properties = {
        "spring.profiles.active": "prod",
        "server.address": "127.0.0.1",
        "server.port": str(server_port),
        "spring.datasource.url": "jdbc:postgresql://127.0.0.1:" + pg_port + "/security_test",
        "spring.datasource.username": "postgres",
        "spring.datasource.password": "test-only",
        "spring.liquibase.enabled": "true",
        "spring.liquibase.contexts": "security-review",
        "spring.data.redis.host": "127.0.0.1",
        "spring.data.redis.port": redis_port,
        "spring.data.redis.database": "0",
        "alt.tenant.store.url": "jdbc:postgresql://127.0.0.1:" + pg_port + "/security_tenants",
        "alt.tenant.store.username": "postgres",
        "alt.tenant.store.password": "test-only",
        "alt.tenant.liquibase.enabled": "false",
        "security.jwt-auth.base64-secret": base64.b64encode(secrets.token_bytes(192)).decode(),
        "security.jwt-auth.base64-secret-refresh": base64.b64encode(secrets.token_bytes(384)).decode(),
        "security.captcha-auth.validation": "true",
        "security.rbac.client.enabled": "false",
        "oss.s3.endpoint": (report / "native-oss").as_uri(),
    }
    with (report / "native-runtime.log").open("w") as output:
        process = subprocess.Popen(
            [str(binary), *arguments.native_arg, *["--" + k + "=" + v for k, v in properties.items()]],
            cwd=root,
            stdout=output,
            stderr=subprocess.STDOUT,
        )
    for _ in range(100):
        if process.poll() is not None:
            raise RuntimeError("Native process exited before HTTP startup; inspect native-runtime.log")
        try:
            response = request("/v1/auth/admin")
            if response[0] == 401:
                break
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(1)
    else:
        raise RuntimeError("Native HTTP startup timed out")
    expect("anonymous_denied", response, 401)
    sql("""
        CREATE EXTENSION IF NOT EXISTS pgcrypto;
        INSERT INTO main_user (user_id,username,password,nick_name,tenant_by,is_enabled,is_deleted)
        VALUES (900001,'native-upgrade','{bcrypt}' || crypt('native-test-only',gen_salt('bf')),'native','master',true,false);
        INSERT INTO main_role (role_id,code,tenant_by,is_enabled,is_deleted)
        VALUES (900010,'native-review-role','master',true,false);
        INSERT INTO main_user_role (user_role_id,user_id,role_id,tenant_by,is_deleted)
        VALUES (900011,900001,900010,'master',false);
        INSERT INTO main_role_permission (role_permission_id,role_id,permission_id,tenant_by,is_deleted)
        VALUES (900012,900010,10054,'master',false);
        """)
    expect(
        "missing_captcha_denied",
        request("/v1/login/jwt", "POST", {"username": "native-upgrade", "password": "native-test-only"}),
        400,
    )
    login = login_with_captcha()
    expect("real_login", login, 200)
    access = json.loads(login[1])["data"]["accessToken"]
    expect("authorized_request", request("/v1/auth/admin", token=access), 200)
    prefix, signature = access.rsplit(".", 1)
    modified = prefix + "." + ("A" if signature[0] != "A" else "B") + signature[1:]
    expect("tampered_token_denied", request("/v1/auth/admin", token=modified), 401)
    sql("UPDATE main_user SET is_enabled=false WHERE user_id=900001;")
    expect("disabled_account_denied", request("/v1/auth/admin", token=access), 401)
    sql("UPDATE main_user SET is_enabled=true WHERE user_id=900001;")
    login = login_with_captcha()
    expect("reenabled_account_login", login, 200)
    access = json.loads(login[1])["data"]["accessToken"]
    docker("exec", redis, "redis-cli", "FLUSHDB")
    expect("revoked_cached_token_denied", request("/v1/auth/admin", token=access), 401)
    (report / "native-smoke-results.json").write_text(
        json.dumps({"passed": True, "binary": str(binary), "checks": checks}, indent=2)
    )
finally:
    if process is not None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
    for container in (pg, redis):
        subprocess.run(["docker", "rm", "-f", container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
