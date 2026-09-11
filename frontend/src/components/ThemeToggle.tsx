import { useState } from "react";
import { getTheme, toggleTheme } from "../theme";

export function ThemeToggle() {
  const [theme, setTheme] = useState(getTheme());
  const koyu = theme === "dark";
  return (
    <button
      type="button"
      className="theme-toggle"
      title={koyu ? "Açık temaya geç" : "Koyu temaya geç"}
      aria-label="Tema değiştir"
      onClick={() => setTheme(toggleTheme())}
    >
      {koyu ? "☀️" : "🌙"}
    </button>
  );
}
