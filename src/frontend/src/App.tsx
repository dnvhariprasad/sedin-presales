import { Navigate, Route, Routes } from "react-router-dom"

import { ProtectedRoute } from "@/features/auth"
import { HomePage } from "@/pages/home-page"
import { LoginPage } from "@/pages/login-page"

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <HomePage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
