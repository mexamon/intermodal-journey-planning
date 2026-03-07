// extract-files.cjs  —  iki üst klasörden itibaren .ts/.js/.tsx dosyalarını toplar

// node extract-files.ts "../../" "output.txt
const fs   = require("fs");
const path = require("path");

// ---------- Parametreler ----------
const args = process.argv.slice(2);
const sourceDir = args[0]
  ? path.resolve(process.cwd(), args[0])
  : path.resolve(__dirname, "../../");          // <‑‑ iki üst klasör
const outputFile = args[1]
  ? path.resolve(process.cwd(), args[1])
  : path.resolve(__dirname, "./output/project-files.txt");

// ---------- Hazırlık ----------
const outputDir = path.dirname(outputFile);
if (!fs.existsSync(outputDir)) fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(outputFile, "", "utf-8");      // eski içeriği sil

const allowed = [".ts", ".js", ".tsx", ".css", ".scss", ".json"];
const ignore  = ["node_modules", ".git", "dist", "build", "coverage"];

function write(filePath, content) {
  const rel = path.relative(path.resolve(__dirname, "../.."), filePath);
  const block = `--- File: ${rel} ---\n${content}\n\n`;
  console.log(`📝  ${rel}`);
  fs.appendFileSync(outputFile, block, "utf-8");
}

function scan(dir) {
  for (const item of fs.readdirSync(dir)) {
    const full = path.join(dir, item);
    const stat = fs.statSync(full);

    if (stat.isDirectory() && !ignore.includes(item)) {
      scan(full);
    } else if (stat.isFile() && allowed.includes(path.extname(item))) {
      write(full, fs.readFileSync(full, "utf-8"));
    }
  }
}

console.log(`🔍 Kaynak:  ${sourceDir}`);
console.log(`📄 Çıktı:   ${outputFile}\n`);
scan(sourceDir);
console.log("\n✅ Tamamlandı.");
