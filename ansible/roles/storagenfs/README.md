# Role Storage NFS

## Mô tả

Role này có nhiệm vụ cài đặt và cấu hình máy chủ NFS Server.

## Chức năng

Role sẽ thực hiện các công việc sau:

- Cài đặt NFS Server.
- Tạo thư mục chia sẻ.
- Triển khai file cấu hình exports.
- Khởi động dịch vụ NFS.
- Thiết lập NFS tự khởi động cùng hệ điều hành.

## Máy chủ áp dụng

```
storagenfs
```


## Luồng hoạt động

```text
Playbook
    │
    ▼
Role Storage NFS
    │
    ▼
Cài NFS Server
    │
    ▼
Tạo thư mục chia sẻ
    │
    ▼
Triển khai exports
    │
    ▼
Khởi động dịch vụ
    │
    ▼
Hoàn thành
```