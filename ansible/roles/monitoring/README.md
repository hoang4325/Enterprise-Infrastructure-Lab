# Role Monitoring

## Mô tả

Role này có nhiệm vụ chuẩn bị máy chủ Monitoring phục vụ giám sát toàn bộ hạ tầng.

## Chức năng

Role sẽ thực hiện các công việc sau:

- Kiểm tra Docker đã được cài đặt.
- Tạo thư mục lưu trữ Monitoring.
- Triển khai cấu hình Prometheus.
- Triển khai Docker Compose.
- Khởi chạy Prometheus và Grafana.


## Máy chủ áp dụng
```
monitor
```


## Luồng hoạt động
```text
Playbook
    │
    ▼
Role Monitoring
    │
    ▼
Kiểm tra Docker
    │
    ▼
Tạo thư mục Monitoring
    │
    ▼
Triển khai Prometheus
    │
    ▼
Triển khai Docker Compose
    │
    ▼
Khởi chạy Monitoring Stack
```