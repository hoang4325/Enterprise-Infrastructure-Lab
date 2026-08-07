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
- Tùy chọn tạo database và tài khoản ứng dụng chỉ được Web Server sử dụng.

## Tài khoản ứng dụng

Mật khẩu không đặt trong role. Tạo file `ansible/group_vars/database/vault.yml`
từ file `.example`, điền mật khẩu đủ dài rồi mã hóa:

```bash
cp ansible/group_vars/database/vault.yml.example ansible/group_vars/database/vault.yml
ansible-vault encrypt ansible/group_vars/database/vault.yml
```

Khi có biến `mariadb_application_password`, role sẽ tạo database `enterprise_app`
và user `webapp` chỉ được phép từ IP Web Server với quyền đọc/ghi cơ bản.

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
