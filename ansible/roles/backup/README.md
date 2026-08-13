# Role Backup

## Mục đích

Role bảo vệ hai loại dữ liệu:

- Dữ liệu NFS được archive tại `storagenfs`.
- Database `enterprise_app` được dump tại `mariadb`, sau đó ghi qua NFS vào
  disk backup riêng trên `storagenfs`.

## Lịch chạy

Hai systemd timer chạy mỗi ngày lúc 02:00:

```text
backup-nfs.timer
backup-mariadb.timer
```

Kiểm tra timer và chạy thủ công:

```bash
systemctl list-timers 'backup-*'
sudo systemctl start backup-nfs.service
sudo systemctl start backup-mariadb.service
```

## Restore validation

Restore MariaDB vào database tạm, còn NFS được giải nén vào thư mục tạm.
Không có script nào ghi đè dữ liệu production.

```bash
sudo /usr/local/sbin/validate-mariadb-restore.sh
sudo /usr/local/sbin/validate-nfs-restore.sh
```

Hoặc chạy validation từ máy Ansible:

```bash
ansible-playbook -i inventory.ini playbooks/backup_restore_validation.yml --ask-vault-pass
```

Mỗi backup có file `.sha256`; validation luôn kiểm tra checksum trước khi
giải nén hoặc import.
