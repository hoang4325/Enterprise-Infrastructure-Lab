# Runbook: Service bị dừng

## Phạm vi

Áp dụng cho HAProxy, Nginx, MariaDB, NFS, Node Exporter và Monitoring Stack.

## 1. Xác định service và máy bị lỗi

```bash
ansible all -i ansible/inventory.ini -b -m service_facts
ansible <host> -i ansible/inventory.ini -b -m shell -a \
  'systemctl --failed --no-pager'
```

## 2. Kiểm tra log trước khi restart

```bash
ansible <host> -i ansible/inventory.ini -b -m shell -a \
  'systemctl status <service> --no-pager; journalctl -u <service> -n 100 --no-pager'
```

Với Monitoring Stack:

```bash
ansible monitor -i ansible/inventory.ini -b -m shell -a \
  'cd /opt/monitoring && docker compose ps && docker compose logs --tail=100'
```

## 3. Kiểm tra cấu hình trước khi khởi động

```bash
ansible loadbalancer -i ansible/inventory.ini -b -m shell -a \
  'haproxy -c -f /etc/haproxy/haproxy.cfg'
ansible webserver -i ansible/inventory.ini -b -m shell -a \
  'nginx -t'
```

## 4. Khôi phục

```bash
ansible <host> -i ansible/inventory.ini -b -m service \
  -a 'name=<service> state=restarted'
```

Sau đó kiểm tra endpoint thật:

```bash
curl -k -I https://10.10.0.210/healthz
curl -k https://10.10.0.210/
```

Nếu service dừng lại ngay sau restart, không restart lặp vô hạn; giữ log và
chuyển sang xử lý theo runbook disk, network hoặc NFS tương ứng.
