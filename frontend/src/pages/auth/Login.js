import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { authApi } from '../../api';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../components/ui/dialog';
import { Eye, EyeOff, Loader2, KeyRound, ShieldAlert } from 'lucide-react';
import trustLogo from '../../assets/trust-logo.jpg';
import loginSlide1 from '../../assets/login-slide-1.webp';
import loginSlide2 from '../../assets/login-slide-2.jpg';
import loginSlide3 from '../../assets/login-slide-3.jpg';
import './Login.css';

const Login = () => {
  const { login } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const [currentSlide, setCurrentSlide] = useState(0);
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [requireNewPassword, setRequireNewPassword] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [changeError, setChangeError] = useState('');
  const [changing, setChanging] = useState(false);
  const backgroundSlides = [loginSlide1, loginSlide2, loginSlide3];

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % backgroundSlides.length);
    }, 5000);
    return () => window.clearInterval(intervalId);
  }, [backgroundSlides.length]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const userData = await login(form.email, form.password);
      if (userData?.mustChangePassword) {
        setRequireNewPassword(true);
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || t('login.loginError'));
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordChangeSubmit = async (e) => {
    e.preventDefault();
    setChangeError('');
    if (!newPassword || newPassword.length < 8) {
      setChangeError('New password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setChangeError('Passwords do not match.');
      return;
    }
    if (newPassword === form.password) {
      setChangeError('New password must differ from the temporary password.');
      return;
    }

    setChanging(true);
    try {
      await authApi.changePassword({
        currentPassword: form.password,
        newPassword: newPassword,
      });
      setRequireNewPassword(false);
      navigate('/dashboard');
    } catch (err) {
      setChangeError(err.response?.data?.message || 'Failed to update password. Please try again.');
    } finally {
      setChanging(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-slideshow">
        {backgroundSlides.map((slide, index) => (
          <div
            key={index}
            className={`slide ${index === currentSlide ? 'active' : ''}`}
            style={{ backgroundImage: `url(${slide})` }}
          />
        ))}
        <div className="overlay" />
      </div>

      <div className="login-card-container">
        <div className="login-box">
          <div className="flex justify-center mb-6">
            <img src={trustLogo} alt="Trust Agro" className="w-20 h-20 rounded-2xl shadow-xl border-2 border-white object-cover" />
          </div>

          <div className="text-center mb-8">
            <h1 className="text-3xl font-black text-gray-900 dark:text-white tracking-tight">{t('login.title')}</h1>
            <p className="text-gray-500 dark:text-gray-400 mt-2 font-medium">{t('login.tagline')}</p>
          </div>

          {error && (
            <div className="bg-red-50 dark:bg-red-950/40 border-l-4 border-red-500 text-red-700 dark:text-red-400 p-4 mb-6 rounded shadow-sm" role="alert">
              <span className="text-sm font-semibold">{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="email" className="text-xs font-bold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                {t('login.email')}
              </Label>
              <Input
                id="email"
                type="email"
                placeholder={t('login.emailPlaceholder')}
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                className="h-12 rounded-xl bg-gray-50 dark:bg-slate-800/80 border-gray-200 dark:border-slate-700 text-foreground focus:bg-white dark:focus:bg-slate-800"
                autoComplete="email"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-xs font-bold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                {t('login.password')}
              </Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder={t('login.passwordPlaceholder')}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  className="h-12 rounded-xl bg-gray-50 dark:bg-slate-800/80 border-gray-200 dark:border-slate-700 text-foreground focus:bg-white dark:focus:bg-slate-800 pr-12"
                  autoComplete="current-password"
                  required
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="absolute right-1 top-1/2 -translate-y-1/2 h-9 w-9 p-0 text-gray-400 hover:text-green-700"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
              </div>
            </div>

            <Button
              type="submit"
              disabled={loading}
              className="w-full h-12 rounded-xl bg-gradient-to-r from-green-700 to-green-600 hover:from-green-800 hover:to-green-700 font-bold shadow-lg shadow-green-900/20"
            >
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t('login.signingIn')}
                </>
              ) : (
                t('login.signIn')
              )}
            </Button>
          </form>

          <div className="mt-8 pt-6 border-t border-gray-100 text-center">
            <p className="text-xs text-gray-400 font-medium">© 2026 Trust Agro Consulting. All rights reserved.</p>
          </div>
        </div>
      </div>

      {/* MANDATORY FIRST-LOGIN PASSWORD CHANGE MODAL */}
      <Dialog open={requireNewPassword} onOpenChange={() => {}}>
        <DialogContent className="sm:max-w-[440px]" onPointerDownOutside={(e) => e.preventDefault()} onEscapeKeyDown={(e) => e.preventDefault()}>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-amber-600">
              <ShieldAlert className="h-5 w-5 text-amber-600" /> First-Time Login Notice
            </DialogTitle>
            <DialogDescription className="text-slate-600 dark:text-slate-300">
              Welcome to Trust Agro ERP! Your temporary password must be changed to a secure personal password before continuing.
            </DialogDescription>
          </DialogHeader>

          {changeError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-xs">
              {changeError}
            </div>
          )}

          <form onSubmit={handlePasswordChangeSubmit} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="new-password">New Password * (min 8 characters)</Label>
              <Input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Enter new password"
                minLength={8}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="confirm-password">Confirm New Password *</Label>
              <Input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter new password"
                minLength={8}
                required
              />
            </div>

            <DialogFooter className="pt-3 border-t">
              <Button type="submit" disabled={changing} className="w-full bg-primary hover:bg-primary/90 flex items-center justify-center gap-2">
                {changing ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" /> Saving...
                  </>
                ) : (
                  <>
                    <KeyRound className="h-4 w-4" /> Save & Access ERP
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Login;
