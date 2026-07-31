# Role: WebServer

## Mục đích

Role này dùng để cấu hình máy chủ Web trong hệ thống.

## Chức năng

- Cài đặt Nginx
- Tạo thư mục mount
- Mount thư mục chia sẻ NFS
- Triển khai cấu hình Nginx
- Khởi động và bật Nginx
- Kiểm tra trạng thái dịch vụ

## Luồng hoạt động

```text
Install Nginx
        │
        ▼
Create Mount Directory
        │
        ▼
Mount NFS Share
        │
        ▼
Deploy nginx.conf
        │
        ▼
Restart Nginx
        │
        ▼
Check Service
```
