export const lightenColor = (color: string, percent: number) => {
  const num = parseInt(color.replace("#", ""), 16);
  const r = (num >> 16) + Math.round(((255 - (num >> 16)) * percent) / 100);
  const g =
    ((num >> 8) & 0x00ff) +
    Math.round(((255 - ((num >> 8) & 0x00ff)) * percent) / 100);
  const b =
    (num & 0x0000ff) + Math.round(((255 - (num & 0x0000ff)) * percent) / 100);

  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, "0")}`;
};

export const darkenColor = (color: string, percent: number) => {
  const num = parseInt(color.replace("#", ""), 16);
  const r = Math.max(
    0,
    (num >> 16) - Math.round(((num >> 16) * percent) / 100)
  );
  const g = Math.max(
    0,
    ((num >> 8) & 0x00ff) - Math.round((((num >> 8) & 0x00ff) * percent) / 100)
  );
  const b = Math.max(
    0,
    (num & 0x0000ff) - Math.round(((num & 0x0000ff) * percent) / 100)
  );

  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, "0")}`;
};
