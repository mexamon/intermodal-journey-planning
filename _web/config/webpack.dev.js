const { merge } = require("webpack-merge");
const common = require("./webpack.common.js");
const paths = require("./paths");
const Dotenv = require("dotenv-webpack");
const ReactRefreshWebpackPlugin = require("@pmmmwh/react-refresh-webpack-plugin");
const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const deps = require("../package.json").dependencies;

module.exports = merge(common, {
  mode: "development",
  output: {
    publicPath: "http://localhost:3041/",
  },
  devtool: "inline-source-map",
  devServer: {
    historyApiFallback: true,
    static: paths.appPublic,
    open: false,
    compress: true,
    hot: true,
    port: 3041,
  },
  plugins: [
    new Dotenv(),
    new ReactRefreshWebpackPlugin(),
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
      },
    }),
  ].filter(Boolean),
});
