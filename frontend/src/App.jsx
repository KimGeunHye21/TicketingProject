import { Routes, Route } from 'react-router-dom';

import LoginPage from './pages/auth/LoginPage';
import OAuthCallbackPage from './pages/auth/OAuthCallbackPage';
import TermsPage from './pages/auth/TermsPage.jsx';
import PrivacyPage from './pages/auth/PrivacyPage';

function App() {
    return (
        <Routes>
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
        </Routes>
    );
}

export default App;