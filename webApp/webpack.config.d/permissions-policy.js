config.devServer = config.devServer || {};
config.devServer.headers = Object.assign({}, config.devServer.headers, {
  "Permissions-Policy": "clipboard-read=(self), clipboard-write=(self)",
});
