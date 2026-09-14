import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { normalizeRole } from '../../lib/permissions';
import LoadingState from '../shared/LoadingState';

/**
 * Route guard supporting role, module, permission, or action-based access.
 *
 * @param {string[]} [roles] - Legacy role allow-list
 * @param {string} [module] - Module key for canAccessModule()
 * @param {string} [permission] - Single permission string
 * @param {string[]} [permissions] - Any-of permission list
 * @param {string} [action] - create | edit | delete | stockIn | stockOut
 * @param {string} [resource] - Resource key for canPerformAction()
 */
const ProtectedRoute = ({ children, roles, module, permission, permissions, action, resource }) => {
  const { user, loading, canAccessModule, hasPermission, hasAnyPermission, canPerformAction } = useAuth();

  if (loading) {
    return <LoadingState message="Loading..." />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.map(normalizeRole).includes(normalizeRole(user.role))) {
    return <Navigate to="/dashboard" replace />;
  }

  if (module && !canAccessModule(module)) {
    return <Navigate to="/dashboard" replace />;
  }

  if (permission && !hasPermission(permission)) {
    return <Navigate to="/dashboard" replace />;
  }

  if (permissions && permissions.length > 0 && !hasAnyPermission(permissions)) {
    return <Navigate to="/dashboard" replace />;
  }

  if (action && resource && !canPerformAction(action, resource)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

export default ProtectedRoute;
