# Runbook: Lỗi NFS

## Dấu hiệu

- Web trả lỗi `404`, `403` hoặc timeout khi đọc nội dung.
- Mount point không còn là filesystem NFS.
- Alert hoặc log cho thấy Web mất kết nối tới Storage.

## 1. Kiểm tra phía Web

```bash
ansible webserver -i ansible/inventory.ini -b -m shell -a \
  'findmnt /var/www/html; mountpoint /var/www/html; nc -vz -w5 10.10.0.240 2049'
```

## 2. Kiểm tra phía Storage

```bash
ansible storagenfs -i ansible/inventory.ini -b -m shell -a \
  'systemctl status nfs-kernel-server --no-pager; exportfs -v; ls -ld /srv/nfs/shared'
```

## 3. Kiểm tra export và firewall

```bash
ansible storagenfs -i ansible/inventory.ini -b -m shell -a \
  'cat /etc/exports; ufw status numbered; ss -lntup | grep 2049'
```

Chỉ Web Server được phép mount export. Nếu sửa `/etc/exports`, hãy cập nhật
biến của role rồi chạy lại Ansible để tránh drift.

## 4. Khôi phục theo thứ tự

```bash
ansible storagenfs -i ansible/inventory.ini -b -m service \
  -a 'name=nfs-kernel-server state=restarted'
ansible storagenfs -i ansible/inventory.ini -b -m shell -a 'exportfs -ra'
ansible webserver -i ansible/inventory.ini -b -m shell -a \
  'mount -a; findmnt /var/www/html'
```

Không xóa dữ liệu trong `/srv/nfs/shared` để xử lý lỗi mount. Nếu dữ liệu bị
hỏng, chuyển sang quy trình backup/restore riêng.
