const convertEffortToHumanReadableFormat = (
  hours: number,
  days: number,
  weeks: number
) => {
  let result = "";

  // Günleri kontrol et
  if (days > 0) {
    if (days >= 5) {
      const weeksFromDays = Math.floor(days / 5);
      const remainingDays = days % 5;
      result += `${weeksFromDays}w `;
      if (remainingDays > 0) {
        result += `${remainingDays}d `;
      }
    } else {
      result += `${days}d `;
    }
  }

  // Saatleri kontrol et
  if (hours > 0) {
    const totalDays = Math.floor(hours / 8) + days;
    const remainingHours = hours % 8;

    if (totalDays > 0) {
      if (totalDays >= 5) {
        const weeksFromDays = Math.floor(totalDays / 5);
        const remainingDays = totalDays % 5;
        result += `${weeksFromDays}w `;
        if (remainingDays > 0) {
          result += `${remainingDays}d `;
        }
      } else {
        result += `${totalDays}d `;
      }
    }

    if (remainingHours > 0) {
      result += `${remainingHours}h `;
    }
  }

  // Haftaları kontrol et
  if (weeks > 0) {
    result += `${weeks}w `;
  }

  const regex = /^(\d+[dhw]\s*)*$/;
  if (!regex.test(result)) {
    alert("NO");
    return "Invalid input! Please use only valid characters (d, h, w) for days, hours, and weeks.";
  }

  return result.trim();
};

export const handleCalculate = (inputValue: string) => {
  const regex = /(\d+)([hdw])/g;
  let match;
  let hours = 0;
  let days = 0;
  let weeks = 0;

  while ((match = regex.exec(inputValue)) !== null) {
    const value = parseInt(match[1], 10);
    const unit = match[2];

    if (unit === "h") {
      hours += value;
    } else if (unit === "d") {
      days += value;
    } else if (unit === "w") {
      weeks += value;
    }
  }

  const result = convertEffortToHumanReadableFormat(hours, days, weeks);
  return result;
};
