// Caches everything the game needs, so it plays with no connection once it has
// been opened once. That matters more than it sounds: a page added to a home
// screen that needs the network is not an app, it is a bookmark.
//
// BUILD_ID is rewritten by assemble.py with a hash of the files below. That is
// not decoration. A browser decides whether to install a new worker by
// byte-comparing this file against the one it has; when the name was a fixed
// "v1" this file was identical every deploy, so no new worker ever installed,
// the cache was never refilled, and everyone who had opened the site once was
// served that first build for ever. A new build must change these bytes.
const BUILD_ID = 'dev';
const CACHE = 'signal-lost-' + BUILD_ID;

const SHELL = [
  './', 'index.html', 'signal-lost.js', 'manifest.webmanifest',
  'icon-192.png', 'icon-512.png',
  'levels/index.txt',
  'levels/level1.txt', 'levels/level2.txt', 'levels/level3.txt',
  'levels/level4.txt', 'levels/level5.txt', 'levels/level6.txt',
  'levels/level7.txt', 'levels/level8.txt', 'levels/level9.txt',
  'levels/level10.txt',
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE)
      .then(cache => cache.addAll(SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(names => Promise.all(names.filter(n => n !== CACHE).map(n => caches.delete(n))))
      .then(() => self.clients.claim())
  );
});

// The page and the game code are fetched fresh when there is a connection and
// fall back to the cache when there is not, so a player who is online always
// gets the current build even if this worker is somehow a stale one. Levels and
// icons stay cache-first: they are the bulk of the bytes, they change rarely,
// and they are what a player on a train actually needs.
function isShellCode(url) {
  return url.pathname.endsWith('/') || url.pathname.endsWith('/index.html')
      || url.pathname.endsWith('/signal-lost.js');
}

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') {
    return;
  }
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) {
    return;
  }

  if (event.request.mode === 'navigate' || isShellCode(url)) {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          const copy = response.clone();
          caches.open(CACHE).then(cache => cache.put(event.request, copy));
          return response;
        })
        .catch(() => caches.match(event.request).then(hit => hit || caches.match('index.html')))
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then(hit => hit || fetch(event.request))
  );
});
