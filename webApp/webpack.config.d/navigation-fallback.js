config.devServer = config.devServer || {};

// Keep the dot rule disabled so a genuine navigation such as /release.v2 still reaches the shell,
// then explicitly leave script/style/icon/manifest/service-worker requests on their resource URL.
const navigationFallback = (context) => {
  const headers = (context.request && context.request.headers) || {};
  const destination = headers["sec-fetch-dest"];
  const acceptsHtml = String(headers.accept || "").includes("text/html");
  const isDocument = destination === "document" || (!destination && acceptsHtml);
  return isDocument ? "/index.html" : context.parsedUrl.pathname;
};

config.devServer.historyApiFallback = {
  disableDotRule: true,
  index: "/index.html",
  rewrites: [{ from: /./, to: navigationFallback }],
};
