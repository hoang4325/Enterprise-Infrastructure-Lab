# Role Firewall

## Mục đích

Role cấu hình UFW theo mô hình phân lớp: HAProxy nhận lưu lượng người dùng,
Web Server truy cập NFS/MariaDB, còn Monitor scrape Node Exporter.

## Luồng rule chính

```text
Mạng quản trị  -> SSH đến mọi máy
Mạng người dùng -> HAProxy:80,443
HAProxy        -> Web Server:80
Web Server     -> MariaDB:3306
Web Server     -> NFS:2049
Monitor        -> Node Exporter:9100
Mạng quản trị  -> Grafana:3000, Prometheus:9090, Alertmanager:9093
```

Role luôn mở SSH trước rồi mới bật chính sách deny incoming để tránh khóa
đường quản trị trong lúc triển khai.
