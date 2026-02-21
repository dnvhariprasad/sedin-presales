import { type ReactNode, useCallback, useEffect, useMemo, useState } from "react"
import axios from "axios"

import { AuthContext, type AuthUser } from "@/features/auth/auth-context"

const TOKEN_KEY = "presales.auth.token"
const USER_KEY = "presales.auth.user"

function loadStoredUser(): AuthUser | null {
  try {
    const value = localStorage.getItem(USER_KEY)
    return value ? (JSON.parse(value) as AuthUser) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Check if token exists on mount and validate
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setUser(null)
    }
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await axios.post("/api/v1/auth/login", { email, password })
      const data = response.data.data ?? response.data
      const { token, email: userEmail, displayName, role } = data

      localStorage.setItem(TOKEN_KEY, token)
      const authUser: AuthUser = {
        email: userEmail,
        displayName,
        role: role as AuthUser["role"],
      }
      localStorage.setItem(USER_KEY, JSON.stringify(authUser))
      setUser(authUser)
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.data) {
        const msg = err.response.data.error ?? err.response.data.message ?? "Invalid credentials"
        setError(typeof msg === "string" ? msg : "Invalid credentials")
      } else {
        setError("Unable to sign in. Please try again.")
      }
    } finally {
      setIsLoading(false)
    }
  }, [])

  const signOut = useCallback(async () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
    setError(null)
  }, [])

  const value = useMemo(
    () => ({
      isAuthenticated: Boolean(user),
      isLoading,
      user,
      error,
      clearError: () => setError(null),
      signIn,
      signOut,
    }),
    [user, isLoading, error, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
