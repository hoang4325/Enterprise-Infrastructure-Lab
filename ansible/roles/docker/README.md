# Role Web Server

## Mô tả
Role này có nhiệm vụ chuẩn bị cấu hình Docker

## Luồng hoạt động
```text
Playbook
    │
    ▼
Role Docker
    │
    ▼
Cài Docker
    │
    ▼
Khởi động Docker
    │
    ▼
Bật Docker khi khởi động hệ thống
    │
    ▼
Kiểm tra trạng thái Docker
    │
    ▼
Hoàn thành
```
