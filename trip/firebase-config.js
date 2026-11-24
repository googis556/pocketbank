// 여행 일정표 전용 Firebase 프로젝트 설정
// pocketbank 프로젝트와 섞이지 않도록 반드시 "새 프로젝트"의 값을 넣으세요.
//
// 값 찾는 곳: Firebase 콘솔 → 새 프로젝트 → 프로젝트 설정(톱니바퀴) → 일반 → 내 앱 → 웹 앱(</>) 추가
// → 나오는 firebaseConfig 객체를 아래 null 자리에 그대로 붙여넣으면 돼요.
// 이 값들은 브라우저에 공개되도록 만들어진 값이라 저장소에 올라가도 괜찮아요.
// (데이터 보호는 trip/firestore.rules 보안 규칙이 담당해요.)
//
// 예시:
// export const firebaseConfig = {
//   apiKey: "...",
//   authDomain: "내프로젝트.firebaseapp.com",
//   projectId: "내프로젝트",
//   storageBucket: "내프로젝트.firebasestorage.app",
//   messagingSenderId: "...",
//   appId: "...",
// };

export const firebaseConfig = null;
