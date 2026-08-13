# Runbook: Các lỗi đã gặp và cách xử lý

Tài liệu này ghi lại các lỗi đã xuất hiện trong quá trình xây dựng lab. Luôn
ưu tiên kiểm tra nguyên nhân trước khi restart service hoặc sửa trực tiếp trên
VM. Sau khi sửa thủ công, phải cập nhật role Ansible để lần chạy sau không bị
drift.

## 1. Firewall chặn luồng giữa các lớp

### Triệu chứng

- NFS mount bị `Connection timed out`.
- HAProxy dự phòng nhận VIP nhưng trả `503 No server is available`.
- `nc` tới port đích thất bại dù service ở máy đích đang active.

### Kiểm tra

```bash
# Chạy từ máy nguồn tới máy đích.
nc -vz -w5 10.10.0.240 2049
nc -vz -w5 10.10.0.220 80

# Kiểm tra rule và socket ở máy đích.
ansible storagenfs -i ansible/inventory.ini -b -m shell -a \
  'ufw status numbered; ss -lntup | grep 2049'
ansible web -i ansible/inventory.ini -b -m shell -a \
  'ufw status numbered; ss -lntup | grep :80'
```

### Cách sửa

Không mở port cho toàn bộ Internet. Thêm rule vào role firewall theo đúng
nguồn và đích:

```text
MariaDB 10.10.0.230 -> Storage 10.10.0.240:2049
HAProxy 10.10.0.210/211 -> Web 10.10.0.220/221:80
Web -> MariaDB:3306
Monitor -> Node Exporter:9100
```

Áp dụng lại:

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/firewall.yml --limit storagenfs
ansible-playbook -i inventory.ini playbooks/firewall.yml --limit web
```

### Xác minh

```bash
ansible mariadb -i inventory.ini -b -m shell -a \
  'timeout 5 bash -c "</dev/tcp/10.10.0.240/2049" && echo NFS_PORT_OPEN'
curl -k -I https://10.10.0.212
```

## 2. NFS mount bị timeout

### Kiểm tra theo thứ tự

```bash
ansible storagenfs -i ansible/inventory.ini -b -m shell -a \
  'systemctl is-active nfs-kernel-server; exportfs -v; ss -lntup | grep 2049'
ansible mariadb -i ansible/inventory.ini -b -m shell -a \
  'ping -c2 10.10.0.240; findmnt /mnt/backup; cat /etc/fstab | grep backup'
ansible web -i ansible/inventory.ini -b -m shell -a \
  'findmnt /var/www/html; mountpoint -q /var/www/html'
```

Nếu server active, export đúng nhưng port timeout, xử lý firewall trước. Nếu
export sai, chạy lại role `storagenfs`; không sửa `/etc/exports` riêng lẻ rồi
bỏ quên source code.

```bash
ansible-playbook -i ansible/inventory.ini playbooks/storagenfs.yml
ansible-playbook -i ansible/inventory.ini playbooks/backup.yml --limit mariadb \
  --ask-vault-pass
```

Web Server mount dữ liệu tại `/var/www/html`, còn MariaDB mount disk backup tại
`/mnt/backup`; kiểm tra nhầm `/mnt/nfs` sẽ cho kết quả sai.

## 3. HAProxy không khởi động

### Kiểm tra

```bash
ansible loadbalancer -i ansible/inventory.ini -b -m shell -a \
  'haproxy -c -f /etc/haproxy/haproxy.cfg; systemctl status haproxy --no-pager; journalctl -u haproxy -n 100 --no-pager'
```

Các nguyên nhân thường gặp:

- Sai đường dẫn hoặc quyền đọc certificate/PEM.
- Backend không đúng địa chỉ hoặc port.
- Cấu hình health check yêu cầu `/healthz` nhưng Web không trả `200`.

Sửa template/biến trong role HAProxy, sau đó chạy:

```bash
ansible-playbook -i ansible/inventory.ini playbooks/haproxy.yml
curl -k -I https://10.10.0.210/healthz
```

## 4. Keepalived chạy nhưng VIP chuyển sang node dự phòng trả 503

### Nguyên nhân

HAProxy2 có VIP nhưng firewall Web chỉ cho phép HAProxy1. Cả hai node trong
group `haproxy` phải được Web Server cho phép truy cập port 80.

### Kiểm tra

```bash
ansible haproxy -i ansible/inventory.ini -b -m shell -a \
  'systemctl is-active keepalived haproxy; ip -br addr'
ansible web -i ansible/inventory.ini -b -m shell -a 'ufw status numbered'
curl -k -i https://10.10.0.212
```

### Khôi phục

Đảm bảo inventory có hai node:

```ini
[haproxy]
loadbalancer ansible_host=10.10.0.210
loadbalancer2 ansible_host=10.10.0.211
```

Sau đó chạy lại firewall và Keepalived:

```bash
ansible-playbook -i ansible/inventory.ini playbooks/firewall.yml --limit web
ansible-playbook -i ansible/inventory.ini playbooks/keepalived.yml \
  --ask-vault-pass
```

## 5. Keepalived báo cần ít nhất hai node

Đây là lỗi cấu hình đúng, không phải lỗi service. VRRP không có ý nghĩa nếu
group `haproxy` chỉ có một máy. Tạo VM thứ hai, đặt IP cùng mạng Layer 2, thêm
vào inventory và kiểm tra:

```bash
ansible haproxy -i ansible/inventory.ini -m ping
```

Không xóa task assert để ép triển khai một node.

## 6. SSH báo `No route to host` hoặc `Destination Host Unreachable`

### Kiểm tra từ máy điều khiển

```bash
ping -c3 10.10.0.220
ip neigh show 10.10.0.220
ssh -o ConnectTimeout=5 hoangnh@10.10.0.220
```

Trên VMware kiểm tra VM đang bật, network adapter đã Connected và dùng đúng
VMnet. Trong console VM:

```bash
ip -br addr
ip route
sudo netplan apply
```

Chỉ chạy lại Ansible sau khi ping và SSH hoạt động. Không nhầm lỗi mạng VM với
lỗi Nginx hoặc HAProxy.

## 7. Ansible báo loop không nhận list

### Lỗi

```text
Invalid data passed to 'loop', it requires a list, got this instead
```

Nguyên nhân thường là dùng `lookup()` trả về chuỗi cho một phần tử. Với dữ
liệu nhiều phần tử, dùng `query()` hoặc `q()`; với biến tĩnh, đảm bảo YAML là
list:

```yaml
docker_users:
  - hoangnh
```

Kiểm tra kiểu dữ liệu trước khi chạy:

```bash
ansible all -i ansible/inventory.ini -m debug \
  -a 'var=docker_users'
```

## 8. Ansible Vault báo file không được mã hóa

### Lỗi

```text
input is not vault encrypted data
```

`ansible-vault edit` chỉ dùng cho file đã mã hóa. Với file thường, mã hóa tại
chỗ bằng:

```bash
ansible-vault encrypt group_vars/<group>/vault.yml
```

Nếu file chưa tồn tại:

```bash
ansible-vault create group_vars/<group>/vault.yml
```

Không commit file Vault chưa mã hóa hoặc token/password vào Git. Kiểm tra:

```bash
head -n1 group_vars/<group>/vault.yml
```

Kết quả phải bắt đầu bằng `$ANSIBLE_VAULT;`.

## 9. Template báo `object of type 'int' has no len()`

Đây thường là `length` đang áp dụng vào biến số, ví dụ Telegram `chat_id`.
Ép kiểu trước khi kiểm tra:

```jinja2
{% set chat_id = alertmanager_telegram_chat_id | default('') | string %}
{% set telegram_enabled = (token | default('') | string | length > 0)
  and (chat_id | length > 0) %}
```

`chat_id` vẫn phải được render thành số trong cấu hình Alertmanager, nhưng khi
kiểm tra độ dài trong Jinja cần chuyển sang chuỗi.

## 10. Restore MariaDB thất bại sau khi checksum đúng

Checksum `OK` chỉ chứng minh file không bị thay đổi. Kiểm tra tiếp:

```bash
ansible mariadb -i ansible/inventory.ini -b -m shell -a \
  'zcat /mnt/backup/mariadb/*.sql.gz | head -80; \
   mariadb -NBe "SELECT table_schema, COUNT(*) FROM information_schema.tables GROUP BY table_schema"'
```

Nếu database `enterprise_app` chưa có bảng, dump chỉ chứa metadata. Restore
vẫn thành công nhưng số bảng bằng `0`; không nên coi đây là file hỏng. Khi ứng
dụng đã có schema, đặt `backup_restore_minimum_tables: 1` và chạy lại:

```bash
ansible-playbook -i ansible/inventory.ini \
  playbooks/backup_restore_validation.yml --ask-vault-pass
```

## 11. Task `apt upgrade` chạy lâu

Kiểm tra có tiến trình apt hoặc khóa dpkg:

```bash
ansible <host> -i ansible/inventory.ini -b -m shell -a \
  'ps -ef | grep -E "apt|dpkg|unattended" | grep -v grep; \
   fuser /var/lib/dpkg/lock-frontend /var/lib/dpkg/lock 2>/dev/null || true'
```

Nếu đang tải package, chờ thêm. Nếu bị gián đoạn, vào VM xử lý:

```bash
sudo dpkg --configure -a
sudo apt-get update
sudo apt-get -s dist-upgrade
```

Không xóa file lock thủ công khi vẫn còn tiến trình apt/dpkg.

## 12. Kiểm tra idempotency sau khi sửa

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/site.yml --ask-vault-pass
ansible-playbook -i inventory.ini playbooks/site.yml --ask-vault-pass
```

Lần chạy thứ hai kỳ vọng `changed=0`. Nếu còn `changed`, dùng `--check --diff`
để tìm task tạo drift:

```bash
ansible-playbook -i inventory.ini playbooks/site.yml \
  --ask-vault-pass --check --diff
```

## Checklist sau khi khôi phục

- Ping và SSH tới máy bị ảnh hưởng hoạt động.
- Service ở trạng thái `active`.
- Port giữa hai lớp được mở đúng nguồn.
- Endpoint thật trả mã `200`.
- Mount point đúng filesystem.
- Alert chuyển sang `resolved`.
- Backup/restore vẫn chạy được.
- Playbook chạy lại không tạo thay đổi bất thường.
- Nguyên nhân và lệnh xử lý đã được ghi vào incident log.
