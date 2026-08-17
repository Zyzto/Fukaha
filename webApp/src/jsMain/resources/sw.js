// Offline app shell for Fukaha. Cleaning links is pure local logic, so once the shell is
// cached the whole app works with no network — including a share that arrives offline.
//
// Strategy: stale-while-revalidate. Kotlin/JS emits fixed filenames (fukaha.js, styles.css),
// so a deploy changes the bytes behind the same URL. Serving from cache and refreshing in the
// background means a launch is instant, and the next launch runs the newly deployed build.
// This only holds because firebase.json marks those files must-revalidate — with immutable
// caching the background fetch would keep returning the old bytes from the HTTP cache.
const CACHE = "fukaha-shell-v2";
// Cairo and the Material Symbols subset are versioned in their URLs by Google Fonts, so they
// are cached forever under a separate name and never revalidated. Without this the icon font
// would be missing offline and every glyph would fall back to its ligature name.
const FONT_CACHE = "fukaha-fonts-v1";
const FONT_HOSTS = ["fonts.googleapis.com", "fonts.gstatic.com"];
const SHELL = [
  "/",
  "/index.html",
  "/styles.css",
  "/fukaha.js",
  "/manifest.webmanifest",
  "/icons/icon.svg",
  "/icons/icon-192.png",
  "/icons/icon-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(SHELL.map((url) => new Request(url, { cache: "reload" }))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  const keep = [CACHE, FONT_CACHE];
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => !keep.includes(key)).map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

function revalidate(request) {
  return fetch(request)
    .then((response) => {
      if (response && response.ok && response.type === "basic") {
        const copy = response.clone();
        caches.open(CACHE).then((cache) => cache.put(request, copy));
      }
      return response;
    })
    .catch(() => undefined);
}

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);

  if (FONT_HOSTS.includes(url.hostname)) {
    event.respondWith(
      caches.match(request).then(
        (hit) =>
          hit ||
          fetch(request).then((response) => {
            const copy = response.clone();
            caches.open(FONT_CACHE).then((cache) => cache.put(request, copy));
            return response;
          })
      )
    );
    return;
  }

  // Embedder probes are deliberately no-cors and must never be served from a cache.
  if (url.origin !== self.location.origin) return;

  // A share target navigation lands on "/?text=…", which never matches a cache entry of its
  // own, so the shell is served explicitly. The query string is read by the app, not the cache.
  const cacheKey = request.mode === "navigate" ? "/index.html" : request;

  event.respondWith(
    caches.match(cacheKey).then((hit) => {
      if (hit) {
        event.waitUntil(revalidate(new Request(cacheKey, { cache: "reload" })));
        return hit;
      }
      return fetch(request).catch(() => caches.match("/index.html"));
    })
  );
});
