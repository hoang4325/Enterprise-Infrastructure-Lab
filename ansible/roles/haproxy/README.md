# Role HAProxy

## Mô tả
Role này có nhiệm vụ cài đặt và cấu hình **HAProxy** để làm Load Balancer cho hệ thống

## Luồng hoạt động
```text
                 Playbook
                     │
                     ▼
          roles/haproxy/tasks
                     │
     ┌───────────────┴───────────────┐
     ▼                               ▼
Cài đặt HAProxy              Triển khai cấu hình
                                     │
                                     ▼
                       Cấu hình có thay đổi?
                             │
                 ┌───────────┴───────────┐
                 ▼                       ▼
              Có thay đổi          Không thay đổi
                 │
                 ▼
         Restart HAProxy
                 │
                 ▼
      Enable & Start Service
                 │
                 ▼
             Hoàn thành
```

## Chức năng
Các công việc sẽ được thực hiện:
- Cài đặt HAProxy
- Triển khai file cấu hình HAProxy
- Kích hoạt dịch vụ HAProxy khi hệ thống khởi động
- Khởi động dịch vụ HAProxy
- Tự động khởi động lại khi cấu hình thay đổi

---
## Máy chủ áp dụng
Role này chỉ áp dụng cho nhóm máy chủ:
```text
loadbalancer
```

## Kết quả sau khi chạy
- HAProxy được cài đặt
- File cấu hình được triển khai
- Dịch vụ HAProxy họat động
- HAProxy tự khởi động cùng OS
