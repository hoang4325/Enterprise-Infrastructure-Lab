# Runbook: Mất kết nối giữa các lớp

## Luồng và port cần kiểm tra

```text
Client -> HAProxy 80/443
HAProxy -> Web 80
Web -> MariaDB 3306
Web -> NFS 2049
Monitor -> Node Exporter 9100
```

## 1. Kiểm tra định tuyến và port

```bash
ansible <source_host> -i ansible/inventory.ini -b -m shell -a \
  'ip route; nc -vz -w5 <destination_ip> <port>'
```

## 2. Kiểm tra firewall hai đầu

```bash
ansible <destination_host> -i ansible/inventory.ini -b -m shell -a \
  'ufw status numbered; ss -lntup'
```

Xác nhận rule cho phép đúng nguồn. Không mở port cho `0.0.0.0/0` nếu luồng
chỉ cần giữa hai lớp nội bộ.

## 3. Kiểm tra request theo tầng

```bash
curl -k -I https://10.10.0.210/healthz
ansible webserver -i ansible/inventory.ini -b -m uri \
  -a 'url=http://127.0.0.1/healthz status_code=200'
ansible webserver -i ansible/inventory.ini -b -m wait_for \
  -a 'host=10.10.0.230 port=3306 timeout=5'
ansible monitor -i ansible/inventory.ini -b -m uri \
  -a 'url=http://10.10.0.220:9100/metrics status_code=200'
```

## 4. Khôi phục

Sửa rule bằng Ansible, sau đó chạy lại playbook firewall. Tránh sửa thủ công
trên một máy rồi quên cập nhật source of truth trong repository.
