# Roadmap Rebuild He Thong (Backend First -> Frontend Mock -> Integration)

## 1) Muc tieu
- Giam xung dot nho le (small conflicts) bang cach chia lai cong viec theo phase ro rang.
- Dung mot tracker tap trung de nhin tong the va trang thai tung phan.
- Lam backend on dinh truoc, sau do frontend voi mock + tuong tac, cuoi cung moi ghep API that.
- Giam mat thoi gian restart he thong trong qua trinh sua loi.

## 2) Nguyen tac van hanh
- Mot source of truth: cap nhat tien do duy nhat trong file nay.
- Khong lam da nhiem: moi phase co Definition of Done ro rang.
- Conflict policy:
  - Nho le thi xu ly ngay trong phase dang lam.
  - Neu conflict vuot qua pham vi phase, dua vao muc Blockers va postpone co kiem soat.
- Moi thay doi lon deu theo chu trinh:
  - Design nho -> Implement -> Build/Test nhanh -> Cap nhat tracker.

## 3) Tong quan phase
- Phase 0: On dinh quy trinh + conflict triage.
- Phase 1: Rebuild backend theo module.
- Phase 2: Chinh frontend voi mock data + interaction flow.
- Phase 3: Integration backend/frontend + hardening.

---

## 4) Tracker tong the
| ID | Hang muc | Phase | Trang thai | Uu tien | Dau vao | Dau ra mong doi | Blocker |
|---|---|---|---|---|---|---|---|
| T0-1 | Chot branch strategy va cach merge conflict | 0 | DONE | Cao | Nhanh toan bo branch hien tai | Rule merge ro rang |  |
| T0-2 | Lap danh sach conflict nho le theo nhom file | 0 | DONE | Cao | Git status + diff | Backlog conflict co nhan |  |
| T1-1 | Backend: config + security baseline | 1 | DONE | Cao | config, security, exception | Khoi dong on dinh, auth pass smoke test |  |
| T1-2 | Backend: entity + repository consistency | 1 | DONE | Cao | entity, repository | Build pass, khong canh bao quan trong |  |
| T1-3 | Backend: service layer refactor theo domain | 1 | TODO | Cao | service | Logic ro, test duong chinh |  |
| T1-4 | Backend: controller + dto contract cleanup | 1 | TODO | Cao | controller, dto | Contract API on dinh |  |
| T1-5 | Backend: seed + migration + startup flow | 1 | TODO | Trung binh | DataSeeder, config DB | Startup nhanh, seed co dieu kien |  |
| T2-1 | Frontend: map man hinh theo domain | 2 | TODO | Cao | UI flow hien tai | Danh sach man hinh + state map |  |
| T2-2 | Frontend: dong bo mock models voi API contract | 2 | TODO | Cao | model mock, DTO map | Mock data sat voi API that |  |
| T2-3 | Frontend: interaction states (loading, empty, error) | 2 | TODO | Cao | tung screen | UX on dinh, khong ket UI |  |
| T2-4 | Frontend: test luong nguoi dung chinh | 2 | TODO | Trung binh | script thao tac | Danh sach luong pass/fail |  |
| T3-1 | Integration: thay mock bang API that theo tung man | 3 | TODO | Cao | backend done + frontend mock done | Tich hop theo dot, rollback duoc |  |
| T3-2 | Integration: E2E smoke (auth -> feed -> post -> interact) | 3 | TODO | Cao | app + backend running | E2E pass |  |
| T3-3 | Hardening: logs, retry, timeout, fallback | 3 | TODO | Trung binh | log + issue list | Giam loi runtime, de debug |  |

Trang thai hop le: TODO | IN_PROGRESS | BLOCKED | REVIEW | DONE

---

## 5) Phase 0 chi tiet (On dinh quy trinh + conflict triage)
### 5.1 Task checklist
- [x] Chot branch chinh de rebuild (de xuat: feed lam integration branch tam).
- [x] Chot quy tac commit message (de xuat: WIP/BACKEND/FRONTEND/INTEGRATION).
- [ ] Chia conflict backlog theo nhom:
  - [x] Backend config/security
  - [x] Backend domain community
  - [x] Backend domain chat/match/profile
  - [x] Frontend community
  - [x] Frontend navigation/state
- [ ] Dat gioi han conflict fix moi ngay (de tranh drift): max 3 conflict chunks/ngay.

### 5.3 Quy tac da chot (T0-1)
- Branch van hanh:
  - `feed`: branch tich hop tam cho phase rebuild.
  - Moi task xong phai commit truc tiep tren `feed`.
- Commit convention:
  - `WIP:` cho task dang test
  - `BACKEND:` cho task backend da chot
  - `FRONTEND:` cho task frontend mock/interaction
  - `INTEGRATION:` cho task ghep API that
- Merge conflict handling:
  - Uu tien giai conflict theo pham vi phase hien tai.
  - Conflict ngoai phase: ghi vao Blockers board, khong sua lan.

### 5.4 Conflict backlog theo nhom file (T0-2)
| Nhom | File/chung loai | Van de xay ra | Muc do | Cach xu ly phase |
|---|---|---|---|---|
| Backend config/security | `application.yaml`, auth/security config | Drift config, secret handling, env mismatch | High | Chot env placeholder + profile secret tach rieng trong Phase 1 |
| Backend community service | controller/service/dto/repository community | Lech contract API, conflict method signature | High | Chot contract DTO truoc, sau do dong bo service/controller |
| Backend entity/repository | `User`, `Post`, `Comment`, repo methods | Builder default + relation mismatch + query naming | Medium | Uu tien consistency entity truoc khi sua service |
| Backend chat/match/profile | service layer khac domain | Conflict import, style, nullability nho le | Medium | Chia task theo module, commit nho va compile nhanh |
| Frontend community | `CommunityScreen`, `AddPostScreen`, `PostManagementScreen`, VM | UI state conflict, luong anh/post conflict | High | Chot mock interaction state truoc, API that sau |
| Frontend navigation/state | `NavGraph`, route wiring, back stack | Route drift, back-stack behavior conflict | High | Khoa route contract trong Phase 2, test back stack tung luong |
| Build/run workflow | startup emulator/backend lap lai | Mat thoi gian restart khi sua loi nho | Medium | Dung chu trinh compile nhanh + smoke test theo dot |

### 5.2 Definition of Done
- [ ] Co danh sach conflict backlog ro rang.
- [ ] Co quy tac merge/commit thong nhat.
- [ ] Khong con sua file ngoai pham vi phase.

---

## 6) Phase 1 chi tiet (Backend rebuild)
### 6.1 Thu tu uu tien de lam
1. Config + Security + Exception handling baseline.
2. Entity + Repository consistency.
3. Service layer theo domain.
4. Controller + DTO contract cleanup.
5. Seeder + startup perf + smoke test.

### 6.2 Tracker theo module backend
| Module | Noi dung | Trang thai | Ghi chu |
|---|---|---|---|
| Config | application, CORS, env, cloud setup | DONE | Da chuan hoa env placeholders + CORS baseline |
| Security | JWT, auth context, role check | DONE | Da bo sung allowlist route + 401/403 JSON handler |
| Exception | AppException + global handler | DONE | Da co baseline @RestControllerAdvice su dung duoc |
| Entity | Builder default, relation, nullable | DONE | Da chuan hoa builder defaults cho PetProfile + quan he collection |
| Repository | query method naming + indexes logic | DONE | Da ra soat consistency naming, khong phat hien blocker |
| Service-Community | post/comment/like/report | TODO |  |
| Service-Chat | group/direct flow | TODO |  |
| Service-Match/Profile | logic nghiep vu cot loi | TODO |  |
| Controller | endpoint contract, status code | TODO |  |
| DTO | request/response alignment | TODO |  |
| DataSeeder | seed co dieu kien, khong gay cham startup | TODO |  |

### 6.3 Definition of Done
- [ ] Backend compile pass.
- [ ] Smoke API pass cho domain chinh.
- [ ] Khong co blocker critical trong service/controller.
- [ ] Startup backend on dinh, khong can restart nhieu lan khi fix nho.

---

## 7) Phase 2 chi tiet (Frontend voi mock + interaction)
### 7.1 Muc tieu phase
- Chot UX va state transitions bang mock truoc.
- Chuan hoa loading/empty/error/disabled states cho cac man hinh chinh.
- Dam bao navigation khong ket luong, back stack dung ky vong.

### 7.2 Task checklist
- [ ] Lap danh sach man hinh uu tien theo luong user:
  - [ ] Auth
  - [ ] Match/Explore
  - [ ] Community feed/post
  - [ ] Chat
  - [ ] Profile
- [ ] Chuan hoa mock schema theo DTO backend.
- [ ] Chuan hoa interaction components:
  - [ ] empty-state
  - [ ] error-state
  - [ ] retry action
  - [ ] disabled/loading action
- [ ] Chay test thao tac tay cho cac luong chinh.

### 7.3 Definition of Done
- [ ] Flow mock truoc khi goi API that da muot.
- [ ] Khong con ket UI khi chon anh, post, dieu huong.
- [ ] Co bang ket qua pass/fail theo man hinh.

---

## 8) Phase 3 chi tiet (Integration)
### 8.1 Chien luoc ghep
- Ghep theo tung dot nho, uu tien domain Community truoc.
- Moi dot ghep gom 3 buoc:
  - Buoc 1: Bat API that cho 1 man hinh.
  - Buoc 2: Chay smoke flow domain do.
  - Buoc 3: Cap nhat tracker + ghi issue ton dong.

### 8.2 Integration checklist
- [ ] Auth + token flow on dinh.
- [ ] Community feed + tao bai + like/comment co du lieu that.
- [ ] Match/Explore + profile mapping dung.
- [ ] Chat flow khong vo state.
- [ ] E2E smoke pass.

### 8.3 Definition of Done
- [ ] Frontend khong con phu thuoc mock cho luong chinh.
- [ ] Build backend + mobile pass.
- [ ] Danh sach loi con lai duoc phan loai ro rang (critical/non-critical).

---

## 9) Keep Tracker hang ngay
## 9.1 Daily log template
| Ngay | Task ID | Viec da lam | Ket qua | Loi/Blocker | Buoc tiep theo |
|---|---|---|---|---|---|
| 2026-04-07 | T0-1 | Chot branch `feed`, chot commit convention, chot merge rule | DONE | Push remote bi 403 (quyen repo) | Tiep tuc T0-2 conflict backlog |
| 2026-04-07 | T0-2 | Gom conflict nho le theo 7 nhom file/he thong | DONE | Khong | Bat dau T1-1 backend config + security baseline |
| 2026-04-07 | T1-1 | Chuan hoa SecurityConfig + CORS + route allowlist + config env placeholders | DONE | Khong | Chuyen T1-2 entity + repository consistency |
| 2026-04-07 | T1-2 | Chuan hoa PetProfile defaults/relations + ra soat repository consistency | DONE | Khong | Chuyen T1-3 service layer refactor theo domain |
| YYYY-MM-DD | T?-? | ... | ... | ... | ... |

## 9.2 Blockers board
| ID | Mo ta blocker | Muc do | Huong xu ly | Owner | Trang thai |
|---|---|---|---|---|---|
| B-001 |  | Critical/High/Med/Low |  |  | OPEN |

---

## 10) De xuat giam mat thoi gian restart
- Backend:
  - Chay mot session backend on dinh, uu tien compile nhanh truoc khi restart app.
  - Gom thay doi theo cum nho (1-3 file) roi moi rerun smoke.
- Frontend:
  - Uu tien fix state va navigation voi mock truoc, tranh phu thuoc backend khi chua can.
- Workflow:
  - Moi lan sua loi: cap nhat tracker truoc, sua sau, validate xong danh dau.
  - Neu loi repeat > 2 lan: dung va ghi vao Blockers board de doi huong xu ly.

---

## 11) Quy uoc cap nhat tracker
- Bat dau task: doi trang thai TODO -> IN_PROGRESS.
- Gap chan: IN_PROGRESS -> BLOCKED, ghi ro blocker.
- Lam xong va tu test: IN_PROGRESS -> REVIEW.
- Da verify: REVIEW -> DONE.

