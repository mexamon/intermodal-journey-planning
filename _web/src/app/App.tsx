// File: src/app/App.tsx
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { HubPage } from './HubPage';
import { ThemeContextProvider } from './contexts/ThemeContext';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { LoginPage } from './pages/LoginPage/LoginPage';
import { ToastContainer } from './components/ToastContainer';
import '../styles/main.scss';

const AuthGate: React.FC = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <HubPage /> : <LoginPage />;
};

const App = () => {
  return (
    <ThemeContextProvider>
      <AuthProvider>
        <BrowserRouter>
          <AuthGate />
        </BrowserRouter>
      </AuthProvider>
      <ToastContainer />
    </ThemeContextProvider>
  );
};

export default App;