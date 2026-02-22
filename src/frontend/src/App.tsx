import { Navigate, Route, Routes } from "react-router-dom"

import { ProtectedRoute } from "@/features/auth"
import { AppShell } from "@/components/layout/app-shell"
import { HomePage } from "@/pages/home-page"
import { LoginPage } from "@/pages/login-page"
import { MastersPage } from "@/pages/masters-page"

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<HomePage />} />
        <Route path="/masters" element={<MastersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
