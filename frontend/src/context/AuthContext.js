import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api';
import {
  hasPermissionForUser,
  hasAnyPermissionForUser,
  canAccessModuleForUser,
  canPerformActionForUser,
  normalizeRole,
} from '../lib/permissions';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      authApi.me()
        .then((res) => setUser(res.data.data))
        .catch(() => localStorage.clear())
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    const { token, ...userData } = res.data.data;
    localStorage.setItem('token', token);
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  const hasRole = (...roles) => {
    if (!user) return false;
    const userRole = normalizeRole(user.role);
    return roles.some((r) => normalizeRole(r) === userRole);
  };

  const hasPermission = (permission) => hasPermissionForUser(user, permission);

  const hasAnyPermission = (permissions) => hasAnyPermissionForUser(user, permissions);

  const canAccessModule = (module) => canAccessModuleForUser(user, module);

  const canPerformAction = (action, module) => canPerformActionForUser(user, action, module);

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      logout,
      hasRole,
      hasPermission,
      hasAnyPermission,
      canAccessModule,
      canPerformAction
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
