import type { ReactNode } from "react"
import { Navigate } from "react-router-dom"

import { useAuth } from "@/features/auth/auth-context"

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#f8fafc]">
        {/* Background Decorative Elements */}
        <div className="absolute inset-0 z-0 pointer-events-none">
          <div className="absolute -left-[10%] -top-[10%] h-[50%] w-[50%] rounded-full bg-blue-50/50 blur-[120px]" />
          <div className="absolute -right-[10%] -bottom-[10%] h-[50%] w-[50%] rounded-full bg-teal-50/50 blur-[120px]" />
        </div>

        <div className="relative z-10 flex flex-col items-center">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-white p-3 shadow-lg ring-1 ring-black/5 animate-pulse">
            <img
              alt="Sedin logo"
              className="h-full w-full object-contain grayscale opacity-50"
              src="/sedin-logo.png"
            />
          </div>
          <div className="flex flex-col items-center gap-2">
            <div className="h-1 w-32 overflow-hidden rounded-full bg-slate-200">
              <div className="h-full w-1/2 animate-[loading_1.5s_infinite_ease-in-out] rounded-full bg-slate-900" />
            </div>
            <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-slate-400">
              Verifying Access
            </p>
          </div>
        </div>

        <style>{`
          @keyframes loading {
            0% { transform: translateX(-100%); }
            100% { transform: translateX(200%); }
          }
        `}</style>
      </main>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}
