# Runbook vận hành hạ tầng

Các runbook dưới đây dùng cho bốn nhóm sự cố thường gặp trong lab:

- [Service bị dừng](service-down.md)
- [Filesystem đầy](disk-full.md)
- [Mất kết nối giữa các lớp](network-unreachable.md)
- [Lỗi NFS](nfs-failure.md)
- [Backup và Restore](backup-restore.md)
- [Keepalived/VRRP](keepalived.md)
- [Các lỗi đã gặp và cách xử lý](troubleshooting-history.md)
- [Kiểm thử nghiệm thu toàn bộ](acceptance-test.md)
- [Triển khai Todo Spring Boot](todo-app.md)

## Nguyên tắc xử lý chung

1. Ghi nhận thời gian, máy bị ảnh hưởng và alert đang firing.
2. Kiểm tra trạng thái trước khi restart hoặc xóa dữ liệu.
3. Thực hiện thay đổi nhỏ nhất có thể và lưu lại lệnh đã chạy.
4. Xác minh từ lớp dưới lên lớp trên: network, service, request thực tế.
5. Sau khi khôi phục, kiểm tra alert đã resolved và ghi lại nguyên nhân.
