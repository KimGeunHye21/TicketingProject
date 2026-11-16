import { Routes, Route } from 'react-router-dom';

import LoginPage from './pages/auth/LoginPage';
import OAuthCallbackPage from './pages/auth/OAuthCallbackPage';
import TermsPage from './pages/auth/TermsPage.jsx';
import PrivacyPage from './pages/auth/PrivacyPage';
import LogoutPage from './pages/auth/LogoutPage';
import ProtectedRoute from './components/ProtectedRoute';
import WithdrawPage from './pages/auth/WithdrawPage';
import EventListPage from './pages/event/EventListPage';
import EventDetailPage from './pages/event/EventDetailPage';
import QueuePage from './pages/queue/QueuePage';
import SectionSelectPage from './pages/booking/SectionSelectPage';

function App() {
    return (
        <Routes>

            {/* 로그인 없어도 접근 가능 */}
            {/* 로그인 없어도 접근 가능 */}

            <Route
                path="/" element={<EventListPage />}
            />

            <Route
                path="/login" element={<LoginPage />}
            />

            <Route
                path="/oauth/google/callback" element={<OAuthCallbackPage />}
            />

            <Route
                path="/auth/terms" element={<TermsPage />}
            />

            <Route
                path="/auth/privacy" element={<PrivacyPage />}
            />


            {/* 공연 */}

            <Route
                path="/events" element={<EventListPage />}
            />

            <Route
                path="/events/:eventId" element={<EventDetailPage />}
            />



            {/* 로그인 필요 */}
            {/* 로그인 필요 */}

            <Route
                path="/logout" element={
                    <ProtectedRoute>
                        <LogoutPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/withdraw" element={
                    <ProtectedRoute>
                        <WithdrawPage />
                    </ProtectedRoute>
                }
            />

            {/* 대기열 */}
            <Route
                path="/queue/events/:eventId/sessions/:sessionId" element={
                    <ProtectedRoute>
                        <QueuePage />
                    </ProtectedRoute>
                }
            />

            {/* 구역 선택 */}
            <Route
                path="/booking/events/:eventId/sessions/:sessionId/sections" element={
                    <ProtectedRoute>
                        <SectionSelectPage />
                    </ProtectedRoute>
                }
            />



        </Routes>
    );
}

export default App;