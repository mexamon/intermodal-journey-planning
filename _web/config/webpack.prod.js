const { merge } = require("webpack-merge");
const common = require("./webpack.common.js");
const paths = require("./paths");
const Dotenv = require("dotenv-webpack");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const deps = require("../package.json").dependencies;

module.exports = merge(common, {
  mode: "production",
  devtool: false,
  output: {
    path: paths.appBuild,
    publicPath: "/",
    filename: "js/[name].[contenthash].bundle.js",
  },
  plugins: [
    new Dotenv(),
    new ModuleFederationPlugin({
      name: "boilerrumHub",
      filename: "remoteEntry.js",
      exposes: {
        "./HubPage": `${paths.appSrc}/app/App.tsx`,
      },
      shared: {
        ...deps,
        react: {
          singleton: true,
          requiredVersion: deps.react,
        },
        "react-dom": {
          singleton: true,
          requiredVersion: deps["react-dom"],
        },
        "react-router-dom": {
          singleton: true,
          requiredVersion: deps["react-router-dom"],
        },
        "qrcode.react": {
          singleton: true,
          requiredVersion: deps["qrcode.react"],
        },
      },
    }),
  ],
  optimization: {
    minimize: true,
    minimizer: [new TerserPlugin(), new CssMinimizerPlugin(), `...`],
  },
  performance: {
    hints: false,
    maxEntrypointSize: 512000,
    maxAssetSize: 512000,
  },
});