// File: babel.config.js (DÜZELTİLMİŞ)
module.exports = {
  presets: [
    "@babel/preset-env",
    ["@babel/preset-react", { runtime: "automatic" }],
    "@babel/preset-typescript",
  ],
  plugins: [
    ["@babel/plugin-proposal-decorators", { legacy: true }],
    ["@babel/plugin-proposal-class-properties", { loose: true }],
    // DÜZELTME: Bu satır, async/await gibi modern JS özelliklerinin
    // doğru çalışması için gereklidir.
    "@babel/plugin-transform-runtime",
  ],
};