# Enterprise Todo App

Ứng dụng Todo Spring Boot dùng để kiểm thử quy trình DevOps trên lab:

```text
Client -> VIP/HAProxy -> Nginx -> Spring Boot -> MariaDB
                                      |
                                      -> Prometheus metrics
```

## API

- `GET /todo/`: giao diện Todo.
- `GET /api/todos`: lấy danh sách.
- `POST /api/todos`: tạo Todo với body `{"title":"..."}`.
- `PUT /api/todos/{id}`: cập nhật Todo.
- `DELETE /api/todos/{id}`: xóa Todo.
- `/actuator/health/readiness`: readiness của ứng dụng và database.
- `/actuator/prometheus`: metrics cho Prometheus.

## Chạy local

```bash
mvn spring-boot:run
```

Ứng dụng cần MariaDB và các biến `SPRING_DATASOURCE_*`. Khi deploy bằng
Ansible, các biến này được render từ Vault trên Web Server.
