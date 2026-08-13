# Runbook: Backup và Restore

## Kiểm tra lịch backup

```bash
ansible storage -i ansible/inventory.ini -b -m shell -a \
  "systemctl list-timers 'backup-*'"
ansible database -i ansible/inventory.ini -b -m shell -a \
  "systemctl list-timers 'backup-*'"
```

## Chạy backup thủ công

```bash
ansible storagenfs -i ansible/inventory.ini -b -m command \
  -a 'systemctl start backup-nfs.service'
ansible mariadb -i ansible/inventory.ini -b -m command \
  -a 'systemctl start backup-mariadb.service'
```

Kiểm tra file và checksum:

```bash
ansible storagenfs -i ansible/inventory.ini -b -m shell -a \
  "find /srv/backup -type f -printf '%p %s bytes\\n'"
```

## Kiểm thử restore

```bash
cd ansible
ansible-playbook -i inventory.ini playbooks/backup_restore_validation.yml \
  --ask-vault-pass
```

MariaDB được restore vào `enterprise_app_restore_test` rồi tự xóa. NFS được
giải nén vào thư mục tạm rồi tự xóa. Nếu checksum sai hoặc không có bảng sau
khi import, playbook sẽ fail.

## Xử lý backup thất bại

1. Kiểm tra timer và trạng thái service.
2. Kiểm tra dung lượng `/srv/backup`.
3. Kiểm tra NFS mount trên MariaDB.
4. Đọc log tại `/srv/backup/logs/`.
5. Chạy lại backup thủ công sau khi khắc phục.
6. Chỉ xóa file backup khi đã có bản khác được kiểm tra restore.
