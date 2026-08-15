import { Routes, Route } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage';
import OAuthCallbackPage from './pages/auth/OAuthCallbackPage';

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
                path="/oauth/callback"
                element={<OAuthCallbackPage />}
            />

        </Routes>
    );
}


export default App;