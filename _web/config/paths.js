const path = require("path");
const fs = require("fs");

// Projenin ana dizinini güvenli bir şekilde bulur
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

module.exports = {
  // Source Files
  appSrc: resolveApp("src"),

  // Production Build Files
  appBuild: resolveApp("build"),

  // Static files that get copied to build folder
  appPublic: resolveApp("public"),

  // Diğer MFE'lerle tutarlılık için ek yollar
  appHtml: resolveApp("public/index.html"),
  appIndexJs: resolveApp("src/index.js"),
  appPackageJson: resolveApp("package.json"),
};
