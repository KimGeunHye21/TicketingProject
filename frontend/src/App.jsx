import { Routes, Route } from 'react-router-dom';

import LoginPage from './pages/auth/LoginPage';
import OAuthCallbackPage from './pages/auth/OAuthCallbackPage';
import TermsPage from './pages/auth/TermsPage.jsx';
import PrivacyPage from './pages/auth/PrivacyPage';
import LogoutPage from './pages/auth/LogoutPage';
import ProtectedRoute from './components/ProtectedRoute';
import WithdrawPage from './pages/auth/WithdrawPage';

function App() {
    return (
        <Routes>

            {/* 로그인 없어도 접근 가능 */}

            <Route
                path="/"
                element={<h1>메인 페이지</h1>}
            />

            <Route
                path="/login"
                element={<LoginPage />}
            />

            <Route
                path="/oauth/google/callback"
                element={<OAuthCallbackPage />}
            />

            <Route
                path="/auth/terms"
                element={<TermsPage />}
            />

            <Route
                path="/auth/privacy"
                element={<PrivacyPage />}
            />

            {/* 로그인 필요 */}

            <Route
                path="/logout"
                element={
                    <ProtectedRoute>
                        <LogoutPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/withdraw"
                element={
                    <ProtectedRoute>
                        <WithdrawPage />
                    </ProtectedRoute>
                }
            />
        </Routes>
    );
}

export default App;