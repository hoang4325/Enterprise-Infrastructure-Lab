# Runbook: Triển khai Todo Spring Boot

## Mục tiêu

Triển khai cùng một phiên bản Todo app lên `webserver` và `webserver2`. Nginx
proxy giao diện `/todo/`, API `/api/` và readiness `/readyz` vào container
Spring Boot ở localhost port `8080`.

## Điều kiện

- Docker Engine và Compose plugin đã hoạt động trên hai Web Server.
- MariaDB hoạt động và có `mariadb_application_password` trong Vault.
- NFS đã mount tại `/var/www/html`.
- Hai Web Server được phép truy cập MariaDB port `3306`.

## Triển khai

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/todo_app.yml \
  --ask-vault-pass
```

Playbook tạo bảng `todos` bằng quyền socket quản trị trước, sau đó build image
và chạy container bằng user không phải root.

## Kiểm tra

```bash
curl -k -i https://10.10.0.212/todo/
curl -k -i https://10.10.0.212/readyz
curl -k https://10.10.0.212/api/todos
curl -k -X POST https://10.10.0.212/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"Kiểm tra HAProxy"}'
curl -k https://10.10.0.212/api/todos
```

Kiểm tra từ từng Web Server:

```bash
ansible web -i inventory.ini -b -m shell -a \
  'cd /opt/enterprise-todo && docker compose ps && curl -fsS http://127.0.0.1:8080/actuator/health/readiness'
```

## Rollback

Đổi image tag trong `ansible/roles/todo_app/defaults/main.yml`, rồi chạy lại
playbook. Dữ liệu Todo nằm ở MariaDB nên đổi container không xóa dữ liệu.

```yaml
todo_app_image: enterprise-todo:0.1.0
```

Sau rollback, kiểm tra `/readyz`, API và request qua VIP.

## Xử lý lỗi

```bash
ansible web -i inventory.ini -b -m shell -a \
  'cd /opt/enterprise-todo && docker compose ps && docker compose logs --tail=100'
ansible web -i inventory.ini -b -m shell -a \
  'curl -i http://127.0.0.1:8080/actuator/health/readiness'
```

- `503` từ Nginx: kiểm tra container và port `127.0.0.1:8080`.
- Readiness `DOWN`: kiểm tra MariaDB port `3306`, tài khoản và database.
- API mất dữ liệu giữa hai Web Server: kiểm tra cả hai node dùng cùng MariaDB.
