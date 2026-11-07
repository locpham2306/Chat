import { BrowserRouter, Route, Routes, Navigate } from 'react-router-dom'
import { AuthLayout } from '~/layouts/auth-layout'
import { MainLayout } from '~/layouts/main-layout'
import { ProtectedRoute } from '~/routes/protected-route'
import { ChatPage } from '~/pages/chat-page'
import { LoginPage } from '~/pages/login-page'
import { RegisterPage } from '~/pages/register-page'
import { NotFoundPage } from '~/pages/not-found-page'

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public auth routes */}
        <Route element={<AuthLayout />}>
          <Route path='/login' element={<LoginPage />} />
          <Route path='/register' element={<RegisterPage />} />
        </Route>

        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path='/' element={<Navigate to='/chat' replace />} />
            <Route path='/chat' element={<ChatPage />} />
            <Route path='/chat/:conversationId' element={<ChatPage />} />
          </Route>
        </Route>

        {/* 404 route */}
        <Route path='*' element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
