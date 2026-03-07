import React, {
  useState,
  useContext,
  createContext,
  ReactNode,
  useEffect,
} from "react";

type ThemeOptions = "dark" | "light";

export interface IBoilerTheme {
  variant: ThemeOptions;
  primaryColor: string;
  borderRadius: number;
  fontSize: number;
}

interface ThemeContextProps {
  theme: IBoilerTheme;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextProps | undefined>(undefined);

export const useThemeContext = (): ThemeContextProps => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useThemeContext must be used within a ThemeContextProvider");
  }
  return context;
};

export const ThemeContextProvider = ({ children }: { children: ReactNode }) => {
  const [theme, setTheme] = useState<IBoilerTheme>({
    variant: "light",
    primaryColor: "#7c3aed",
    borderRadius: 6,
    fontSize: 14,
  });

  useEffect(() => {
    // SCSS :global(.dark) veya :global(.light) seçicilerinin çalışması için
    // en üst seviyedeki elemente tema sınıfını ekliyoruz.
    const body = document.body;
    body.classList.remove("dark", "light");
    body.classList.add(theme.variant);
  }, [theme]);
  
  const toggleTheme = () => {
    setTheme(prevTheme => ({
        ...prevTheme,
        variant: prevTheme.variant === 'light' ? 'dark' : 'light'
    }));
  };

  const value = {
    theme,
    toggleTheme,
  };

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
};
