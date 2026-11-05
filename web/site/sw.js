// Caches everything the game needs, so it plays with no connection once it has
// been opened once. That matters more than it sounds: a page added to a home
// screen that needs the network is not an app, it is a bookmark.
const CACHE = 'signal-lost-v1';

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE).then(cache => cache.addAll([
      './', 'index.html', 'signal-lost.js', 'manifest.webmanifest',
      'icon-192.png', 'icon-512.png',
      'levels/index.txt',
      'levels/level1.txt', 'levels/level2.txt', 'levels/level3.txt',
      'levels/level4.txt', 'levels/level5.txt', 'levels/level6.txt',
      'levels/level7.txt', 'levels/level8.txt', 'levels/level9.txt',
      'levels/level10.txt',
    ])).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(names => Promise.all(names.filter(n => n !== CACHE).map(n => caches.delete(n))))
      .then(() => self.clients.claim())
  );
});

// Cache first: the game is the same every time, and a player on a train
// should not notice the difference.
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then(hit => hit || fetch(event.request))
  );
});
