// File: config/webpack.common.js
const path = require('path');
const paths = require('./paths');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const autoprefixer = require('autoprefixer');

module.exports = {
  entry: [paths.appIndexJs],

  output: {
    path: paths.appBuild,
    filename: '[name].bundle.js',
  },

  plugins: [
    new CleanWebpackPlugin(),
    new MiniCssExtractPlugin({
      filename: 'styles/[name].[contenthash].css',
      chunkFilename: '[id].[contenthash].css',
    }),
    new CopyWebpackPlugin({
      patterns: [
        {
          from: `${paths.appPublic}/assets`,
          to: 'assets',
          globOptions: { ignore: ['*.DS_Store'] },
          noErrorOnMissing: true,
        },
      ],
    }),
    new HtmlWebpackPlugin({
      title: 'THY Intermodal Planner',
      favicon: `${paths.appPublic}/favicon.png`,
      template: paths.appHtml,
      filename: 'index.html',
    }),
  ],

  module: {
    rules: [
      {
        test: /\.(ts|tsx|js|jsx)$/,
        exclude: /node_modules/,
        use: 'babel-loader',
      },

      /* ---------- 1. Sadece *.module.(s)css  =  CSS Modules ---------- */
      {
        test: /\.module\.(css|scss|sass)$/,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: 'css-loader',
            options: {
              /* *** BURASI ÖNEMLİ *** */
              modules: {
                namedExport: true,                        // <‑‑  bütün class’ları named export olarak çıkar
                exportLocalsConvention: 'camelCase',      //    .foo-bar -> fooBar
                localIdentName: '[name]__[local]--[hash:base64:5]',
              },
              esModule: true,                              // esm gerekiyorsa
              sourceMap: true,
            },
          },
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: { plugins: [autoprefixer()] },
            },
          },
          {
            loader: 'sass-loader',
            options: {
              additionalData: `@use "${path
                .resolve(__dirname, '../src/styles/variables.scss')
                .replace(/\\/g, '/')}" as *;`,
            },
          },
        ],
      },

      /* ---------- 2. Diğer tüm (s)css dosyaları ---------- */
      {
        test: /\.(css|scss|sass)$/,
        exclude: /\.module\.(css|scss|sass)$/,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: { plugins: [autoprefixer()] },
            },
          },
          'sass-loader',
        ],
      },

      { test: /\.svg$/, use: ['@svgr/webpack'] },
      { test: /\.(?:ico|gif|png|jpg|jpeg)$/i, type: 'asset/resource' },
      { test: /\.(woff2?|eot|ttf|otf)$/, type: 'asset/inline' },
    ],
  },

  resolve: {
    extensions: ['.tsx', '.ts', '.jsx', '.js', '.json'],
  },
};
