# Role Monitoring

## Mô tả

Role này có nhiệm vụ chuẩn bị máy chủ Monitoring phục vụ giám sát toàn bộ hạ tầng.

## Chức năng

Role sẽ thực hiện các công việc sau:

- Kiểm tra Docker đã được cài đặt.
- Tạo thư mục lưu trữ Monitoring.
- Triển khai cấu hình Prometheus.
- Triển khai rule cảnh báo CPU, RAM, filesystem và Node Exporter.
- Triển khai Alertmanager để gom nhóm và định tuyến cảnh báo.
- Cấu hình Grafana Data Source tự động.
- Tự động nạp dashboard tổng quan hạ tầng.
- Lưu dữ liệu Prometheus và Grafana bằng Docker volume.
- Triển khai Docker Compose.
- Khởi chạy Prometheus và Grafana.

Alertmanager chạy tại:

```text
http://10.10.0.250:9093
```

Telegram được bật khi truyền `alertmanager_telegram_bot_token` và
`alertmanager_telegram_chat_id` qua Ansible Vault hoặc biến an toàn. Khi chưa
có hai giá trị này, Alertmanager vẫn nhận và hiển thị cảnh báo nhưng không gửi
ra ngoài.


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
