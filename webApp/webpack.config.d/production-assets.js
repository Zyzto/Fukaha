if (config.mode === "production") {
  config.output = Object.assign({}, config.output, {
    clean: true,
    chunkFilename: "fukaha.[name].[contenthash:12].js",
    filename: "fukaha.[contenthash:12].js",
  });

  config.optimization = Object.assign({}, config.optimization, {
    chunkIds: "deterministic",
    concatenateModules: true,
    minimize: true,
    moduleIds: "deterministic",
    realContentHash: true,
    usedExports: true,
  });
}
