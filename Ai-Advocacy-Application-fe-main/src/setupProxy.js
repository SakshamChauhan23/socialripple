const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function setupProxy(app) {
  app.use(
    "/v1",
    createProxyMiddleware({
      target: "http://localhost:8081",
      changeOrigin: true,
      ws: true,
    })
  );

  app.use(
    "/external-ingestion",
    createProxyMiddleware({
      target: "http://localhost:8080",
      changeOrigin: true,
      pathRewrite: { "^/external-ingestion": "" },
    })
  );

  app.use(
    "/ws",
    createProxyMiddleware({
      target: "ws://localhost:8081",
      changeOrigin: true,
      ws: true,
    })
  );
};
