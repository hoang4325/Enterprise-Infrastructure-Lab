# Role Docker

## Mô tả

Role này cài đặt và cấu hình Docker Engine trên các máy chủ cần chạy container.
Trong lab hiện tại, role được dùng cho nhóm `webserver` và `monitoring`.

## Luồng hoạt động

```text
Playbook docker.yml
    |
    v
Role docker
    |
    v
Configure Docker repository
    |
    v
Install Docker Engine
    |
    v
Enable and start Docker service
    |
    v
Add users to docker group
    |
    v
Configure Docker daemon
    |
    v
Verify Docker installation
```


