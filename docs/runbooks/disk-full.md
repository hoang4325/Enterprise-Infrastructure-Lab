# Runbook: Filesystem đầy

## Dấu hiệu

Alert `HostFilesystemAlmostFull` cảnh báo từ 85%, còn
`HostFilesystemCritical` cảnh báo từ 95%.

## 1. Xác định filesystem và thư mục chiếm dung lượng

```bash
ansible <host> -i ansible/inventory.ini -b -m shell -a \
  'df -hT; du -xhd1 /var /opt 2>/dev/null | sort -h | tail -30'
```

Kiểm tra riêng Docker trên máy monitor:

```bash
ansible monitor -i ansible/inventory.ini -b -m shell -a \
  'docker system df; du -xhd1 /var/lib/containerd /var/lib/docker 2>/dev/null | sort -h'
```

## 2. Dọn dữ liệu an toàn

Xóa log đã rotate hoặc cache không cần thiết theo chính sách lưu trữ. Với
Docker, chỉ dọn image không còn được container sử dụng sau khi kiểm tra:

```bash
ansible monitor -i ansible/inventory.ini -b -m shell -a \
  'docker image prune -af'
```

Không dùng `docker volume prune` nếu chưa xác nhận volume không chứa dữ liệu
Prometheus, Grafana hoặc Alertmanager.

## 3. Kiểm tra cấu hình ngăn tái diễn

- Prometheus đã giới hạn retention theo ngày và dung lượng.
- Docker đã giới hạn log JSON ở 10 MB, tối đa 3 file.
- Nếu vẫn vượt 85%, mở rộng disk/LVM thay vì tăng ngưỡng alert.

## 4. Xác minh

```bash
ansible <host> -i ansible/inventory.ini -b -m shell -a 'df -h /'
curl -s http://10.10.0.250:9090/api/v1/alerts
```

Alert chỉ được xem là xử lý xong khi filesystem dưới ngưỡng và alert chuyển
sang `resolved` sau thời gian đánh giá tương ứng.
