importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyDyv9Cy7GAUlY78E7P0_sEINjYXlxsoXx4",
  authDomain: "pocketbank-da202.firebaseapp.com",
  projectId: "pocketbank-da202",
  storageBucket: "pocketbank-da202.firebasestorage.app",
  messagingSenderId: "290510794044",
  appId: "1:290510794044:web:f3c2d4dba29ab58bdc8239"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || '포켓뱅크 알림';
  const body = payload.notification?.body || '';
  self.registration.showNotification(title, {
    body,
    icon: '/pocketbank/icon-192.png',
    badge: '/pocketbank/icon-192.png'
  });
});
