# Role MariaDB

## Mô tả

Role này có nhiệm vụ cài đặt và cấu hình MariaDB Server.


## Chức năng

Role sẽ thực hiện các công việc sau:

- Cài đặt MariaDB Server.
- Khởi động dịch vụ MariaDB.
- Thiết lập MariaDB khởi động cùng hệ điều hành.
- Triển khai file cấu hình.
- Khởi động lại MariaDB nếu cấu hình thay đổi.

## Máy chủ áp dụng

```
mariadb
```

## Luồng hoạt động

```text
Playbook
    │
    ▼
Role MariaDB
    │
    ▼
Cài MariaDB
    │
    ▼
Triển khai cấu hình
    │
    ▼
Khởi động MariaDB
    │
    ▼
Enable Service
    │
    ▼
Hoàn thành
```