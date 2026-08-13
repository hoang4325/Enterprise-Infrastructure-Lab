# Runbook: CI/CD Todo app bằng GitHub Actions

## Luồng

```text
Pull Request -> Maven test -> Gitleaks -> dependency check -> Docker build -> Trivy
Push main    -> các bước CI -> push image SHA vào GHCR -> rolling deploy Ansible
```

Image production có dạng:

```text
ghcr.io/<github-owner>/enterprise-todo:<git-sha>
```

Tag dùng commit SHA nên một deployment luôn truy vết và rollback được về đúng
artifact đã build, không dùng tag `latest`.

## Workflow

- `.github/workflows/ci.yml`: chạy trên GitHub-hosted runner cho Pull Request
  và khi push vào `main`.
- `.github/workflows/cd.yml`: chỉ chạy sau khi CI của `main` thành công, hoặc
  chạy tay từ tab Actions. Deploy chạy `serial: 1`: Web Server 1 phải readiness
  thành công rồi mới cập nhật Web Server 2.

## Tạo self-hosted runner

Runner cần nằm trên máy có SSH tới toàn bộ VM lab, ví dụ máy `devops` đang chạy
Ansible. Trong GitHub repository, mở **Settings -> Actions -> Runners -> New
self-hosted runner**, chọn Linux rồi dùng chính lệnh GitHub cung cấp để tải và
đăng ký runner.

Khi đăng ký, gán label `lab`. Kiểm tra runner đã có các công cụ sau:

```bash
ansible --version
ansible-galaxy collection list | grep -E 'community.docker|community.mysql'
curl --version
```

Chạy runner như service theo hướng dẫn GitHub. Không chạy runner bằng tài khoản
root. Tài khoản runner phải có quyền đọc repository và SSH đến các VM bằng key
được inject từ GitHub Secret trong mỗi job.

## GitHub Secrets

Tại **Settings -> Secrets and variables -> Actions**, tạo các repository
secrets sau:

| Secret | Nội dung |
|---|---|
| `LAB_SSH_PRIVATE_KEY` | Private key SSH của user quản trị VM |
| `ANSIBLE_VAULT_PASSWORD` | Mật khẩu Ansible Vault |

`GITHUB_TOKEN` được GitHub tạo tự động trong mỗi job, dùng để push/pull GHCR.
Sau lần CI đầu tiên, kiểm tra package `enterprise-todo` trong GitHub Packages
được liên kết với repository này. Nếu package để private, cấp quyền read cho
repository để token của workflow và Web Server pull image được.

## Bật quyền GitHub Actions

Trong **Settings -> Actions -> General**, đặt Workflow permissions thành
`Read and write permissions` để job CI có thể push image vào GHCR.

## Kích hoạt pipeline

1. Tạo branch `feature/todo-change` và sửa source trong `app/`.
2. Push branch, mở Pull Request vào `main`.
3. Chờ CI pass: Maven test, secret scan, dependency check và Trivy scan.
4. Merge Pull Request vào `main`.
5. CI push image lên GHCR; CD tự deploy rolling qua runner `lab`.

Kiểm tra kết quả bằng:

```bash
curl -k -i https://10.10.0.212/readyz
curl -k https://10.10.0.212/api/todos
```

## Deploy tay

Vào **Actions -> CD -> Run workflow**. Workflow dùng commit đang chọn và vẫn
deploy image theo SHA. Chỉ dùng cách này sau khi image của commit đó đã tồn tại
trong GHCR.

## Rollback

Lấy SHA của bản deploy tốt trước đó trong GitHub Actions hoặc Git log. Chạy
workflow `CD` tay cho commit đó. Ansible pull đúng image tag SHA và cập nhật
từng Web Server.

Sau rollback, xác minh:

```bash
curl -k --fail https://10.10.0.212/readyz
ansible web -i ansible/inventory.ini -b -m shell -a \
  'cd /opt/enterprise-todo && docker compose images'
```

## Xử lý lỗi CD

```bash
# Trên Ansible controller/self-hosted runner.
ansible-playbook -i ansible/inventory.ini ansible/playbooks/todo_app.yml \
  --ask-vault-pass \
  -e 'todo_app_image=ghcr.io/<owner>/enterprise-todo:<sha>' \
  -e 'todo_app_build_image=false' \
  -e 'todo_app_registry_enabled=true'

# Trên một Web Server.
sudo -i
cd /opt/enterprise-todo
docker compose ps
docker compose logs --tail=100
```

Nếu pull GHCR bị `denied`, kiểm tra package permission và token. Nếu readiness
thất bại, kiểm tra kết nối MariaDB, file `.env` quyền `0600`, log container và
health endpoint trước khi thử deploy lại.
