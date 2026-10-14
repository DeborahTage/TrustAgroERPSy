import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Eye, EyeOff, Loader2 } from 'lucide-react';
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
      await login(form.email, form.password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || t('login.loginError'));
    } finally {
      setLoading(false);
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
    </div>
  );
};

export default Login;
