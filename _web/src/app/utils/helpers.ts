import { KeyboardEvent } from "react";

export const onPressEnter = (
  event: KeyboardEvent<HTMLInputElement>,
  fn: () => void
) => {
  if (event.key === "Enter") {
    fn();
  }
};
