# 여행 일정표 (`/trip`)

함께 짜는 여행 일정표예요. 회원가입(개인 이름 또는 단체 이름) → 여행 만들기 → 시간표에서 드래그하거나 시간을 입력해 일정 추가 → 링크로 공유 → 이미지(PNG)·PDF로 내보내기.

- 링크를 받은 사람은 **로그인 없이 보기**만 할 수 있어요.
- 링크로 들어와서 **로그인하면 자동으로 편집자**가 돼요.
- 여행 삭제는 만든 사람만 할 수 있어요.

## 처음 한 번 설정 (pocketbank와 별도 Firebase 프로젝트)

1. [Firebase 콘솔](https://console.firebase.google.com)에서 **새 프로젝트**를 만들어요. 예: `trip-planner`
2. **Authentication** → 시작하기 → 로그인 방법 → **이메일/비밀번호** 사용 설정
3. Authentication → 설정 → **승인된 도메인**에 `googis556.github.io` 추가
4. **Firestore Database** → 데이터베이스 만들기 (위치: `asia-northeast3` 서울 추천, 프로덕션 모드)
5. Firestore → **규칙** 탭에 [`firestore.rules`](firestore.rules) 내용을 통째로 붙여넣고 게시
6. 프로젝트 설정(톱니바퀴) → 일반 → 내 앱 → **웹 앱(`</>`) 추가** → 나오는 `firebaseConfig` 값을 [`firebase-config.js`](firebase-config.js)의 `null` 자리에 붙여넣기
7. 이 변경을 `main`에 머지하면 `https://googis556.github.io/pocketbank/trip/` 에서 열려요.

## 파일

| 파일 | 내용 |
| --- | --- |
| `index.html` | 앱 전체 (HTML·CSS·JS 한 파일) |
| `firebase-config.js` | 여행 앱 전용 Firebase 설정값 |
| `firestore.rules` | Firestore 보안 규칙 (여행 앱 프로젝트에만 적용) |

## 데이터 구조

- `trips/{여행ID}`: `title`, `start`(YYYY-MM-DD), `days`, `ownerId`, `members`(uid → 표시 이름), `memberIds`
- `trips/{여행ID}/events/{일정ID}`: `title`, `place`, `memo`, `cat`, `day`, `start`/`end`(0시부터 분 단위), 작성자·수정자
