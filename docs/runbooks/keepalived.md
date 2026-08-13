# Runbook Keepalived/VRRP

## Mục tiêu

Keepalived tạo một Virtual IP (VIP) dùng chung cho hai máy HAProxy. Khi
HAProxy chính hoặc cả máy chính gặp lỗi, VIP chuyển sang máy dự phòng.

## Điều kiện

- Có hai VM HAProxy trong group `haproxy`.
- Hai VM cùng mạng Layer 2/VMnet.
- VIP `10.10.0.212` chưa được máy nào sử dụng.
- Mật khẩu VRRP được lưu trong `group_vars/haproxy/vault.yml`.

## Triển khai

Thêm máy thứ hai vào `ansible/inventory.ini`. Tạo biến
`keepalived_auth_password` trong file biến của group `haproxy`, sau đó mã hóa
file bằng Ansible Vault. Mật khẩu VRRP không được ghi trực tiếp vào role hoặc
Git và nên dài từ 1 đến 8 ký tự theo giới hạn của Keepalived.

```bash
ansible-playbook -i inventory.ini playbooks/keepalived.yml --ask-vault-pass
```

Kiểm tra VIP:

```bash
ansible haproxy -i inventory.ini -b -m shell \
  -a "ip -br addr; systemctl is-active keepalived"
```

## Kiểm thử chuyển VIP

Gọi dịch vụ qua VIP:

```bash
curl -k -I https://10.10.0.212
```

Trên node đang giữ VIP, dừng HAProxy:

```bash
sudo systemctl stop haproxy
```

Sau vài giây, kiểm tra VIP đã xuất hiện trên node còn lại và gọi lại lệnh
`curl`. Cuối cùng bật lại HAProxy và xác nhận trạng thái trở về bình thường.
