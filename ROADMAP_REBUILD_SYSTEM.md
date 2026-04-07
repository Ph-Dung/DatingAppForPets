# Roadmap Rebuild He Thong (Backend First -> Frontend Real Flow -> Hardening)

## 1) Muc tieu
- Giam xung dot nho le (small conflicts) bang cach chia lai cong viec theo phase ro rang.
- Dung mot tracker tap trung de nhin tong the va trang thai tung phan.
- Lam backend on dinh truoc, sau do frontend theo real flow (API that + state + navigation), cuoi cung hardening va E2E.
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
- Phase 2: Hoan thien frontend real flow + state + navigation.
- Phase 3: Hoan thien integration backend/frontend + hardening + E2E.

### 3.1 Phan cong pham vi
| Nhom | Pham vi | Owner | Ghi chu |
|---|---|---|---|
| Backend API core | Quan ly bai viet ca nhan, tuong tac cong dong, bao cao/kiem duyet | Toi | Tap trung vao API, business rule, quyen, response |
| Frontend flows | Auth, Match/Explore, Chat, Profile/Account, Shared UI/runtime | Teammate | Khong nam trong scope lam chinh cua toi |
| Integration coupler | Gan backend va frontend, smoke/E2E | Phoi hop | Chi lam khi backend API cua toi da on dinh |

### 3.2 Ranh gioi backend trong scope nay
- Trong roadmap nay, backend cua toi chi bao gom nhom Community/Report/Moderation cho bai viet va binh luan.
- Cac luong Review, Block, Call, Appointment, Chat service, va Match service la scope khac, chi tham chieu khi can phoi hop contract.
- Neu co endpoint trung ten/gan ten voi scope khac, uu tien doc theo pham vi feature, khong mo rong tac dung ra ngoai roadmap nay.

---

## 4) Tracker tong the
| ID | Hang muc | Phase | Trang thai | Uu tien | Dau vao | Dau ra mong doi | Blocker |
|---|---|---|---|---|---|---|---|
| T0-1 | Chot branch strategy va cach merge conflict | 0 | DONE | Cao | Nhanh toan bo branch hien tai | Rule merge ro rang |  |
| T0-2 | Lap danh sach conflict nho le theo nhom file | 0 | DONE | Cao | Git status + diff | Backlog conflict co nhan |  |
| T1-1 | Backend: config + security baseline | 1 | DONE | Cao | config, security, exception | Khoi dong on dinh, auth pass smoke test |  |
| T1-2 | Backend: entity + repository consistency | 1 | DONE | Cao | entity, repository | Build pass, khong canh bao quan trong |  |
| T1-3 | Backend: service layer refactor theo domain | 1 | DONE | Cao | service | Logic ro, test duong chinh |  |
| T1-4 | Backend: controller + dto contract cleanup | 1 | DONE | Cao | controller, dto | Contract API on dinh |  |
| T1-5 | Backend: seed + migration + startup flow | 1 | DONE | Trung binh | DataSeeder, config DB | Startup nhanh, seed co dieu kien |  |
| T1-6 | Backend: test gate on dinh (tach khoi DB local) | 1 | DONE | Cao | ApplicationTests, test profile | `mvn test` pass on dinh, khong phu thuoc password Postgres local |  |
| TB-1 | Backend scope: B1 Quan ly bai viet ca nhan | 1 | DONE | Cao | post CRUD, owner check | Tao/sua/xoa bai viet on dinh |  |
| TB-2 | Backend scope: B2 Tuong tac cong dong | 1 | DONE | Cao | like/comment/reply | Like/comment/reply dong bo dung |  |
| TB-3 | Backend scope: B3 Bao cao + kiem duyet | 1 | DONE | Cao | report/moderation config | Report luu dung + moderation tuy chon |  |
| T2-1 | Frontend: chot real flow theo domain | 2 | DONE | Cao | UI flow hien tai | Danh sach man hinh + state map + route map + API map baseline |  |
| T2-2 | Frontend: flow Auth chi tiet | 2 | DONE | Cao | login/register/token/session | Auth diem vao app that |  |
| T2-3 | Frontend: flow Match/Explore chi tiet | 2 | DONE | Cao | swipe/filter/match list | Match experience that, khong con mock |  |
| T2-4 | Frontend: flow Community chi tiet | 2 | TODO | Cao | feed/create/edit/delete/engage | Community real flow on dinh |  |
| T2-5 | Frontend: flow Chat chi tiet | 2 | TODO | Cao | direct/group/call/appointment/review | Chat handoff that, khong vo state |  |
| T2-6 | Frontend: flow Profile/Account + shared UI | 2 | TODO | Cao | pet/account/photos/vaccination | Profile+shared runtime on dinh |  |
| T2-7 | Frontend: test luong nguoi dung chinh tren API that | 2 | TODO | Trung binh | script thao tac | Danh sach luong pass/fail |  |
| T3-1 | Integration: thay tung mock con lai bang API that | 3 | TODO | Cao | backend done + frontend API ready | Tich hop theo dot, rollback duoc |  |
| T3-2 | Integration: E2E smoke (auth -> match -> community -> chat -> profile) | 3 | TODO | Cao | app + backend running | E2E pass |  |
| T3-3 | Hardening: logs, retry, timeout, fallback | 3 | TODO | Trung binh | log + issue list | Giam loi runtime, de debug |  |
| T3-4 | Hardening: cleanup code path va canh bao con lai | 3 | TODO | Trung binh | build, lint, warnings | Giam technical debt sau khi ghep that |  |

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
  - `FRONTEND:` cho task frontend real flow/interaction
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
| Frontend community | `CommunityScreen`, `AddPostScreen`, `PostManagementScreen`, VM | UI state conflict, luong anh/post conflict | High | Chot real flow state truoc, API that sau |
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
| Service-Community | post/comment/like/report | DONE | Da tach helper lookup/permission + giam lap logic |
| Service-Chat | group/direct flow | DONE | Da chuan hoa helper user/group/member + check quyen |
| Service-Match/Profile | logic nghiep vu cot loi | DONE | Da giam lap logic lookup pet/user o MatchRequestService |
| Controller | endpoint contract, status code | DONE | Da dong bo status code create/delete + not-found handling |
| DTO | request/response alignment | DONE | Da bo sung validation cho request body thieu @Valid |
| DataSeeder | seed co dieu kien, khong gay cham startup | DONE | Da them config bat/tat seeding + gioi han users + bo upload anh mac dinh |

### 6.3 Definition of Done
- [x] Backend compile pass.
- [x] Smoke API pass cho domain chinh.
- [x] Khong co blocker critical trong service/controller.
- [x] Startup backend on dinh, khong can restart nhieu lan khi fix nho.

Ghi chu verify cuoi Phase 1 (2026-04-07):
- Da chot cach lam hybrid: runtime van dung DB that, test gate dung profile `test` + H2 de on dinh hoa CI/local review.
- Da verify `backend/.\\mvnw.cmd test` PASS tren profile `test`.

### 6.4 Backend API task overview (pham vi toi phu trach)

#### 6.4.1 Task B1 - Quan ly bai viet ca nhan
- Muc tieu: dam bao luong tao/sua/xoa bai viet hoat dong on dinh cho chu so huu bai.
- Pham vi:
  - Tao bai viet (text + anh)
  - Chinh sua bai viet
  - Xoa bai viet
- Dau ra mong doi:
  - CRUD bai viet chay tren API that
  - Kiem tra quyen owner ro rang
  - Feed cap nhat dung sau thao tac
- Kiem tra bat buoc:
  - happy path create/update/delete
  - forbidden/not-found/content rong/upload fail

#### 6.4.2 Task B2 - Tuong tac cong dong
- Muc tieu: dam bao nguoi dung co the tuong tac bai viet va comment theo thoi gian thuc.
- Pham vi:
  - Like/bo like bai viet
  - Gui comment
  - Tra loi comment
- Dau ra mong doi:
  - Like count va comment tree dong bo dung
  - Response du de frontend render ngay
- Kiem tra bat buoc:
  - toggle nhieu lan, reply dung parent, post/comment khong ton tai

#### 6.4.3 Task B3 - Bao cao va kiem duyet
- Muc tieu: ghi nhan bao cao dung doi tuong va ho tro kiem duyet noi dung theo config.
- Pham vi:
  - Bao cao vi pham bai viet/binh luan
  - Lay danh sach report cua chinh user
  - Kiem duyet tu dong (tuy chon, bat/tat theo config)
- Dau ra mong doi:
  - Report luu duoc voi target + reason + reporter
  - Kiem duyet noi dung khong lam vo flow dang bai co ban
- Kiem tra bat buoc:
  - invalid target/reason, moderation on/off, timeout/fallback

#### 6.4.4 Thu tu thuc thi backend
1. B1 - Quan ly bai viet ca nhan.
2. B2 - Tuong tac cong dong.
3. B3 - Bao cao va kiem duyet.
4. Recheck toan bo contract voi frontend team.

#### 6.4.5 Definition of Done rieng cho pham vi cua toi
- API co validate ro rang va response thong nhat.
- Quyen so huu bai viet va permission duoc check day du.
- Feed cap nhat dung sau create/update/delete/like/comment/report.
- Khong con hardcode UI assumptions trong backend response.
- Co test cho happy path va at least 1-2 edge case cho moi flow.

---

## 7) Phase 2 chi tiet (Frontend real flow + interaction)
### 7.1 Muc tieu phase
- Chot UX va state transitions tren API that cho tung luong.
- Chuan hoa loading/empty/error/disabled states theo tung flow, khong lam chung chung.
- Dam bao auth/token, navigation, back stack, va handoff giua cac flow di end-to-end.

### 7.2 Task checklist

#### 7.2.1 Luong 2A - Auth
- Muc tieu: xac thuc user, cap token, khoi phuc session, va dua app vao dung route sau dang nhap.
- Man hinh: Login, Register, logout action, token check khi mo app.
- Dinh tuyen: login -> pet setup neu chua co profile, login -> match/home neu da co profile, register -> setup/profile flow.
- Dau vao: email/password, register info, token luu trong DataStore.
- Dau ra: token hop le, route vao main flow, logout xoa token va ve login.
- Can xu ly: invalid credential, expired token, register fail, duplicate email, restart app khi da co session.
- DoD: mo app xong khong bi day sai man, dang nhap/thoat dang nhap va session resume chay duoc.

#### 7.2.2 Luong 2B - Match/Explore
- Muc tieu: dua user vao train swipe/filter va xem danh sach like/match that.
- Man hinh: MatchSwipe, MatchFilter, MatchedList, WhoLikedMe.
- Dinh tuyen: swipe -> detail/profile, filter -> apply/reset, matched list -> chat/interaction.
- Dau vao: danh sach pet tu backend, filter state, action like/dislike/super like.
- Dau ra: request match gui that, matched list cap nhat, who-liked-me hien du lieu that.
- Can xu ly: het data, filter khong co ket qua, swipe nhanh, request bi tu choi, back to list giu state.
- DoD: swipe/filter khong ket UI, danh sach match/liked me khop backend, route khong mat state.

#### 7.2.3 Luong 2C - Community
- Muc tieu: feed va bai dang phai chay tren API that tu dau cuoi.
- Man hinh: CommunityScreen, AddPostScreen, PostManagementScreen.
- Dinh tuyen: feed -> add post -> manage post -> edit/delete -> like/comment/report.
- Dau vao: feed data, post draft, image picker, comment text, report reason.
- Dau ra: post create/update/delete, like/comment/report luu backend, feed refresh dung.
- Can xu ly: upload anh fail, content rong, update/delete tranh double submit, empty feed, error state.
- DoD: tao bai, sua/xoa bai, like/comment/report va refresh feed deu chay on.

#### 7.2.4 Luong 2D - Chat
- Muc tieu: chat list, direct/group chat, call/appointment/review handoff phai thong.
- Man hinh: ChatList, ChatDetail, GroupChatDetail, CreateGroupChat, CallScreen, AppointmentScreen, ReviewScreen.
- Dinh tuyen: chat list -> direct/group -> call -> appointment -> review.
- Dau vao: conversation list, message draft, group member data, signaling/call params.
- Dau ra: gui nhan tin, tao group, join group, goi call, dat hen, viet review.
- Can xu ly: reconnect websocket, lost signal, conversation refresh, permission/check user, call cancel/end.
- DoD: khong vo state khi vao/ra chat, call/appointment/review handoff ro rang, tin nhan update that.

#### 7.2.5 Luong 2E - Profile/Account
- Muc tieu: hoan thien day du thong tin pet va account de cac flow khac dung du lieu that.
- Man hinh: AccountScreen, MyProfileScreen, EditUserProfileScreen, ChangePasswordScreen, PetProfileSetup/Edit/Detail, PhotoManage, VaccinationList/Form.
- Dinh tuyen: account -> profile edit -> photo/vaccine -> back ve account/home.
- Dau vao: profile info, photo list, vaccination record, account settings.
- Dau ra: profile data cap nhat, doi mat khau, anh/tiem chung dong bo.
- Can xu ly: form validation, upload photo, empty profile, null data, refresh after save.
- DoD: cac flow profile/account luon co data hop le va quay lai dung route.

#### 7.2.6 Luong 2F - Shared UI/runtime
- Muc tieu: chuan hoa cac thanh phan chung de tat ca flow co cung hanh vi runtime.
- Thanh phan: empty-state, error-state, retry action, disabled/loading action, bottom nav, route guard, back stack.
- Dau vao: loading state, error message, permission/auth state, route current.
- Dau ra: UI nhat quan, retry thong minh, khong ket loading, bottom nav chi hien o route can.
- Can xu ly: race condition khi load, double click, route guard sau logout, back button khi dang edit.
- DoD: cac flow con lai co cung behavior, khong co man hinh phat sinh style/runtime rieng.

#### 7.2.7 Thu tu thuc thi
1. Auth.
2. Shared UI/runtime cho auth + main nav.
3. Match/Explore.
4. Community.
5. Chat.
6. Profile/Account.
7. Kiem tra lai handoff giua cac luong.

- [ ] Chuan hoa schema frontend theo DTO backend cho tung luong.
- [ ] Chay test thao tac tay cho tung luong tren app that.

### 7.3 Definition of Done
- [ ] Flow 2A/2B/2C/2D/2E/2F deu chay tren API that.
- [ ] Khong con ket UI khi chon anh, post, dieu huong, call, hoac logout.
- [ ] Co bang ket qua pass/fail theo tung luong.

### 7.4 Giao dien kiem soat theo flow
| Flow | Man hinh / module | API / state phai co | Loi co the gap | Kiem tra bat buoc |
|---|---|---|---|---|
| 2A | Login, Register, token resume | token store, auth status, redirect | 401, expired token, duplicate email | login/logout/restart app |
| 2B | Swipe, Filter, MatchedList, WhoLikedMe | list load, action commit, filter reset | empty list, rapid swipe, stale data | swipe/filter/back |
| 2C | Feed, AddPost, PostManagement | feed refresh, image upload, CRUD | upload fail, double submit, stale feed | create/edit/delete/like/comment |
| 2D | ChatList, ChatDetail, GroupChat, Call, Appointment, Review | socket/ws, history, signaling | reconnect fail, lost signal, dead thread | direct/group/call handoff |
| 2E | Profile, Edit, Photo, Vaccination, Account | save/update/refresh, permission | validation fail, empty profile, upload fail | edit/save/back refresh |
| 2F | Shared runtime | loading/error/retry/nav guard | spinner kẹt, route sai, back stack sai | rotate/reload/logout |

### 7.5 Ma tran thuc thi Phase 2
| Thu tu | Task ID | Flow | Dau vao | Output | Gate hoan tat |
|---|---|---|---|---|---|
| 1 | T2-2 | Auth | login/register/token/session | auth route + session resume | login/logout/restart app |
| 2 | T2-1 | Shared UI/runtime baseline | route map + flow map | baseline flow map on dinh | route map reviewed |
| 3 | T2-3 | Match/Explore | pet list/filter/action state | swipe/filter/match result that | swipe/filter/back |
| 4 | T2-4 | Community | feed/post drafts/image/comment data | CRUD community real flow | create/edit/delete/like/comment |
| 5 | T2-5 | Chat | conversation/socket/call params | direct/group/call handoff | direct/group/call handoff |
| 6 | T2-6 | Profile/Account | profile/photo/vaccine/account data | profile/account save + refresh | edit/save/back refresh |
| 7 | T2-7 | Validation | full app state + all flow data | pass/fail checklist | tat ca flow pass smoke |

### 7.6 Baseline flow map da chot (T2-1)
| Domain | Route chinh | Screen/module chinh | State owner | API source that |
|---|---|---|---|---|
| Auth | `login`, `register`, `pet/setup` | LoginScreen, RegisterScreen, PetProfileSetupScreen | AuthViewModel, PetProfileViewModel | AuthApi, PetApi |
| Match/Explore | `match/swipe`, `match/filter`, `match/liked-me`, `match/matched` | MatchSwipeScreen, MatchFilterScreen, WhoLikedMeScreen, MatchedListScreen | MatchViewModel | MatchApi, PetApi |
| Community | `community`, `community/add`, `community/management` | CommunityScreen, AddPostScreen, PostManagementScreen | CommunityViewModel | CommunityApi |
| Chat | `chat`, `chat/direct/...`, `chat/group/...`, `chat/call/...`, `chat/appointment/...`, `chat/review/...` | ChatListScreen, ChatDetailScreen, GroupChatDetailScreen, CallScreen, AppointmentScreen, ReviewScreen | ChatViewModel | ChatApi, GroupChatApi, CallApi, AppointmentApi, ReviewApi |
| Profile/Account | `pet/me`, `pet/mypet`, `account/edit`, `account/change-password`, `pet/photos`, `pet/vaccinations` | AccountScreen, MyProfileScreen, EditUserProfileScreen, ChangePasswordScreen, PhotoManageScreen, VaccinationList/Form | PetProfileViewModel, UserViewModel | PetApi, UserApi |
| Shared runtime | global nav host + incoming call overlay | PetMatchNavGraph, BottomNav, IncomingCallOverlay | NavController, ChatViewModel | SignalingClient, route guards trong NavGraph |

Checklist lock T2-1:
- [x] Route map va route arguments duoc chot theo NavGraph hien tai.
- [x] State owner duoc map ro theo domain (Auth/Match/Community/Chat/Profile).
- [x] API map da doi chieu voi Retrofit interfaces, khong dung mock repository cho flow chinh.
- [x] Baseline du de bat dau T2-2 -> T2-7 theo thu tu phase.

---

## 8) Phase 3 chi tiet (Integration + hardening)
### 8.1 Chien luoc ghep
- Ghep theo tung dot nho, uu tien theo thu tu luong: Auth -> Match -> Community -> Chat -> Profile.
- Moi dot ghep gom 3 buoc:
  - Buoc 1: Bat API that cho 1 man hinh.
  - Buoc 2: Chay smoke flow domain do.
  - Buoc 3: Cap nhat tracker + ghi issue ton dong.

### 8.2 Integration checklist
- [ ] Auth + token flow on dinh.
- [ ] Match/Explore + profile mapping dung.
- [ ] Community feed + tao bai + like/comment co du lieu that.
- [ ] Chat flow khong vo state.
- [ ] E2E smoke pass.
- [ ] Cleanup warnings va loi con lai khong chan release.

### 8.3 Definition of Done
- [ ] Frontend khong con phu thuoc mock cho luong chinh.
- [ ] Build backend + mobile pass.
- [ ] Danh sach loi con lai duoc phan loai ro rang (critical/non-critical).
- [ ] Co checklist rollback neu 1 flow that gap loi.

---

## 9) Keep Tracker hang ngay
## 9.1 Daily log template
| Ngay | Task ID | Viec da lam | Ket qua | Loi/Blocker | Buoc tiep theo |
|---|---|---|---|---|---|
| 2026-04-07 | T0-1 | Chot branch `feed`, chot commit convention, chot merge rule | DONE | Push remote bi 403 (quyen repo) | Tiep tuc T0-2 conflict backlog |
| 2026-04-07 | T0-2 | Gom conflict nho le theo 7 nhom file/he thong | DONE | Khong | Bat dau T1-1 backend config + security baseline |
| 2026-04-07 | T1-1 | Chuan hoa SecurityConfig + CORS + route allowlist + config env placeholders | DONE | Khong | Chuyen T1-2 entity + repository consistency |
| 2026-04-07 | T1-2 | Chuan hoa PetProfile defaults/relations + ra soat repository consistency | DONE | Khong | Chuyen T1-3 service layer refactor theo domain |
| 2026-04-07 | T1-3 | Refactor Community/GroupChat/MatchRequest service de giam lap va chuan quyen truy cap | DONE | Khong | Chuyen T1-4 controller + dto contract cleanup |
| 2026-04-07 | T1-4 | Chuan hoa controller + DTO contracts, them validation va status code nhat quan | DONE | Khong | Chuyen T1-5 seed + startup flow |
| 2026-04-07 | T1-5 | Toi uu DataSeeder va them app.seed config de giam startup time/restart cost | DONE | Khong | Chuyen sang Phase 2 (frontend real flow + interaction) |
| 2026-04-07 | T1-6 | Chot gate test backend bang profile `test` (H2), tranh phu thuoc password Postgres local | DONE | Khong | San sang bat dau T2-1 |
| 2026-04-08 | T2-1 | Chot baseline real flow frontend: route map + state owner + API map theo domain | DONE | Khong | Chuyen sang T2-2 (Auth chi tiet) |
| 2026-04-08 | T2-2 | Hoan tat Auth flow: login/register + session resume theo token/profile + logout route gate | DONE | Khong | Chuyen sang T2-3 (Match/Explore chi tiet) |
| 2026-04-08 | T2-3 | Hoan tat Match/Explore: loading-error-retry cho swipe/who-liked-me/matched + xu ly like API fail an toan | DONE | Khong | Chuyen sang T2-4 (Community chi tiet) |
| 2026-04-07 | TB-1 | Hoan tat B1 post CRUD (create/update/delete) + owner permission + edge-case test | DONE | Khong | Chuyen sang TB-2 (like/comment/reply) |
| 2026-04-07 | TB-2 | Hoan tat B2 like/comment/reply + edge-case test (toggle, post/comment not-found) | DONE | Khong | Chuyen sang TB-3 (report/moderation) |
| 2026-04-07 | TB-3 | Hoan tat B3 report + moderation test (target validation, unsupported target, moderation on/off) | DONE | Khong | Chuyen sang T3-1 (integration mock -> API that) |
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
  - Uu tien fix state va navigation tren API that, tranh phu thuoc mock khi chua can.
- Workflow:
  - Moi lan sua loi: cap nhat tracker truoc, sua sau, validate xong danh dau.
  - Neu loi repeat > 2 lan: dung va ghi vao Blockers board de doi huong xu ly.

---

## 11) Quy uoc cap nhat tracker
- Bat dau task: doi trang thai TODO -> IN_PROGRESS.
- Gap chan: IN_PROGRESS -> BLOCKED, ghi ro blocker.
- Lam xong va tu test: IN_PROGRESS -> REVIEW.
- Da verify: REVIEW -> DONE.

---

## 12) Final execution plan (Ready to implement)

### 12.1 Uu tien thuc thi trong scope cua toi
1. TB-1 (B1) - Quan ly bai viet ca nhan.
2. TB-2 (B2) - Tuong tac cong dong.
3. TB-3 (B3) - Bao cao + kiem duyet.
4. Recheck contract voi frontend truoc khi chuyen sang Integration Phase 3.

### 12.2 Gate chot tung task backend
| Task | Build gate | API gate | Test gate | Chuyen task |
|---|---|---|---|---|
| TB-1 | `mvn test` pass | create/update/delete post ok | owner/forbidden/not-found | sang TB-2 |
| TB-2 | `mvn test` pass | like/comment/reply ok | toggle/reply invalid parent | sang TB-3 |
| TB-3 | `mvn test` pass | report/moderation ok | invalid target + moderation on/off | sang T3-1 |

### 12.3 Tieu chi roadmap final
- Scope ro: phan backend cua toi da tach theo task overview, khong lan scope teammate.
- Trinh tu ro: co thu tu thuc thi backend va thu tu flow frontend.
- Gate ro: moi task co gate build/API/test de stop-go.
- Co the trien khai ngay: chi can cap nhat daily log + blockers trong qua trinh lam.

