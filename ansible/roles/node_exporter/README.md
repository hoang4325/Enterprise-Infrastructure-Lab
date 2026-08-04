# Node Exporter

Role này cài Prometheus Node Exporter trên toàn bộ máy chủ trong inventory để Prometheus thu thập metrics hệ điều hành.

Role thực hiện:

- Tạo user hệ thống `node_exporter` không có quyền đăng nhập.
- Tải Node Exporter phiên bản cố định và kiểm tra SHA-256.
- Cài systemd service tại cổng `9100`.
- Bật service tự khởi động cùng hệ điều hành.
- Kiểm tra endpoint `/metrics` sau khi triển khai.

Chạy riêng role:

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/node_exporter.yml
```
