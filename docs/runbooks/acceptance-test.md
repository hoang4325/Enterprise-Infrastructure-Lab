# Runbook: Kiểm thử nghiệm thu toàn bộ hạ tầng

## Mục tiêu

Runbook này dùng sau mỗi lần thay đổi lớn để xác nhận hệ thống vẫn hoạt động
theo luồng production-like:

```text
Client -> VIP Keepalived -> HAProxy -> Web Server
                                      -> MariaDB
                                      -> NFS
Prometheus -> Node Exporter
Backup -> Restore validation
```

Các bài kiểm thử dừng service đều phải có bước khôi phục ngay sau đó. Không
thực hiện trong thời gian có người dùng thật hoặc khi backup đang chạy.

## 1. Kiểm tra baseline

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/site.yml --ask-vault-pass
ansible-playbook -i inventory.ini playbooks/site.yml --ask-vault-pass
```

Lần chạy thứ hai phải đạt:

```text
changed=0
failed=0
unreachable=0
```

Kiểm tra tất cả VM còn truy cập được:

```bash
ansible all -i inventory.ini -m ping --ask-vault-pass
```

## 2. Kiểm tra dịch vụ qua VIP

```bash
curl -k -i https://10.10.0.212
```

Kết quả đạt khi trả `HTTP 200` và nội dung ứng dụng hợp lệ.

Xác định node đang giữ VIP:

```bash
ansible haproxy -i inventory.ini -b -m shell -a \
  'systemctl is-active haproxy keepalived; ip -br addr' \
  --ask-vault-pass
```

## 3. Kiểm thử Keepalived failover

Xác định `loadbalancer` đang giữ VIP, sau đó dừng Keepalived trên node chính:

```bash
ssh hoangnh@10.10.0.210 'sudo systemctl stop keepalived'
sleep 5
curl -k -i https://10.10.0.212
```

Xác nhận VIP xuất hiện trên `loadbalancer2`:

```bash
ssh hoangnh@10.10.0.211 'ip -br addr'
```

Khôi phục node chính:

```bash
ssh hoangnh@10.10.0.210 'sudo systemctl start keepalived'
sleep 5
curl -k -i https://10.10.0.212
```

Kết quả nghiệm thu thực tế: VIP đã chuyển sang HAProxy2 và cả trước/sau
failover đều trả `HTTP 200`.

## 4. Kiểm thử Web Server failover

```bash
ssh hoangnh@10.10.0.220 'sudo systemctl stop nginx'
sleep 5
curl -k -i https://10.10.0.212
```

Nếu vẫn trả `HTTP 200`, HAProxy đã loại Web Server1 khỏi backend và chuyển
sang Web Server2. Bật lại service:

```bash
ssh hoangnh@10.10.0.220 'sudo systemctl start nginx'
sleep 5
curl -k -i https://10.10.0.212
```

## 5. Kiểm tra MariaDB outage và phục hồi

Kiểm tra từ Web Server:

```bash
ssh hoangnh@10.10.0.230 'sudo systemctl stop mariadb'
ssh hoangnh@10.10.0.220 'nc -z -w2 10.10.0.230 3306; echo exit=$?'
ssh hoangnh@10.10.0.230 'sudo systemctl start mariadb'
sleep 4
ssh hoangnh@10.10.0.220 'nc -z -w3 10.10.0.230 3306; echo exit=$?'
```

Kết quả mong muốn: khi dừng, port 3306 thất bại; sau khi bật lại, port hoạt
động trở lại. Cần kiểm tra thêm endpoint ứng dụng nếu ứng dụng có truy vấn
database thật.

## 6. Kiểm tra NFS và phục hồi

Kiểm tra mount đúng đường dẫn:

```bash
ansible web -i inventory.ini -b -m shell -a \
  'findmnt /var/www/html; mountpoint -q /var/www/html'
ansible mariadb -i inventory.ini -b -m shell -a \
  'findmnt /mnt/backup; mountpoint -q /mnt/backup'
```

Kiểm tra ngắn việc port NFS mất và phục hồi:

```bash
ssh hoangnh@10.10.0.240 'sudo systemctl stop nfs-kernel-server'
ssh hoangnh@10.10.0.230 'nc -z -w2 10.10.0.240 2049; echo exit=$?'
ssh hoangnh@10.10.0.240 'sudo systemctl start nfs-kernel-server'
sleep 4
ssh hoangnh@10.10.0.230 'nc -z -w3 10.10.0.240 2049; echo exit=$?'
```

Không truy cập file trên mount NFS trong thời gian server đang dừng vì mount
dùng chế độ `hard` có thể chờ lâu. Sau khi NFS hoạt động, chạy lại validation:

```bash
ansible web -i inventory.ini -b -m shell -a \
  'findmnt /var/www/html; test -f /var/www/html/index.html'
```

## 7. Kiểm thử backup và restore

```bash
ssh hoangnh@10.10.0.240 'sudo systemctl start backup-nfs.service'
ssh hoangnh@10.10.0.240 'sudo /usr/local/sbin/validate-nfs-restore.sh'
ssh hoangnh@10.10.0.230 'sudo systemctl start backup-mariadb.service'
ssh hoangnh@10.10.0.230 'sudo /usr/local/sbin/validate-mariadb-restore.sh'
```

Hoặc chạy playbook:

```bash
ansible-playbook -i inventory.ini \
  playbooks/backup_restore_validation.yml --ask-vault-pass
```

Checksum phải trả `OK`. Database hiện tại của lab có thể có `0` bảng; điều đó
vẫn hợp lệ khi `backup_restore_minimum_tables: 0`. Khi ứng dụng có schema,
đặt giá trị thành `1` để validation nghiêm ngặt hơn.

Kiểm tra timer bằng đúng unit trên từng máy:

```bash
ssh hoangnh@10.10.0.240 'systemctl is-active backup-nfs.timer; systemctl list-timers backup-nfs.timer'
ssh hoangnh@10.10.0.230 'systemctl is-active backup-mariadb.timer; systemctl list-timers backup-mariadb.timer'
```

## 8. Kiểm tra monitoring

```bash
ssh hoangnh@10.10.0.250 'for endpoint in \
  http://127.0.0.1:9090/-/ready \
  http://127.0.0.1:9093/-/ready \
  http://127.0.0.1:3000/api/health; do \
  curl -sS -o /dev/null -w "$endpoint %{http_code}\\n" "$endpoint"; \
done'
```

Kiểm tra Node Exporter trên toàn bộ máy:

```bash
ansible all -i inventory.ini -m uri \
  -a 'url=http://127.0.0.1:9100/metrics status_code=200' \
  --ask-vault-pass
```

Khi cần kiểm thử alert, dừng một service trong thời gian ngắn, theo dõi
Prometheus/Alertmanager và bật service lại ngay sau khi xác nhận alert.

## 9. Kiểm tra firewall theo luồng

```bash
ansible web -i inventory.ini -b -m shell -a 'ufw status numbered'
ansible storagenfs -i inventory.ini -b -m shell -a 'ufw status numbered'
```

Các luồng tối thiểu phải được cho phép:

```text
HAProxy1/2 -> Web1/2:80
Web1/2 -> MariaDB:3306
Web1/2 -> Storage:2049
MariaDB -> Storage:2049
Monitor -> Node Exporter:9100
```

## Kết quả nghiệm thu gần nhất

| Hạng mục | Kết quả |
|---|---|
| Playbook chạy lặp | Đạt, `changed=0` |
| Keepalived failover | Đạt, VIP chuyển và HTTP 200 |
| Web failover | Đạt, HTTP 200 |
| MariaDB outage/recovery | Đạt, port 3306 mất rồi hoạt động lại |
| NFS outage/recovery | Đạt, port 2049 mất rồi hoạt động lại |
| NFS backup/restore | Đạt, checksum `OK` |
| MariaDB backup/restore | Đạt, checksum `OK`, 0 bảng hiện tại |
| Prometheus/Alertmanager/Grafana | Đạt, HTTP 200 |
| Node Exporter | Đạt trên 7 máy |

Sau khi hoàn tất, lưu thời điểm kiểm thử, người thực hiện và log vào hồ sơ
thay đổi hoặc incident log.
