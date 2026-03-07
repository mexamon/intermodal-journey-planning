export const hexToRgb = (hex: string) => {
  console.log("hex =>", hex);
  const cleanedHex = hex.replace(/^#/, "");

  if (!/^([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/.test(cleanedHex)) {
    return null;
  }

  const fullHex =
    cleanedHex.length === 3
      ? cleanedHex
          .split("")
          .map((char) => char + char)
          .join("")
      : cleanedHex;

  const r = parseInt(fullHex.slice(0, 2), 16);
  const g = parseInt(fullHex.slice(2, 4), 16);
  const b = parseInt(fullHex.slice(4, 6), 16);

  return { r, g, b };
};
