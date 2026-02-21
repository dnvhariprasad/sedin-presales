import { createContext, useContext } from "react"

export type UserRole = "ADMIN" | "EDITOR" | "VIEWER"

export interface AuthUser {
  email: string
  displayName: string
  role: UserRole
}

export interface AuthContextValue {
  isAuthenticated: boolean
  isLoading: boolean
  user: AuthUser | null
  error: string | null
  clearError: () => void
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error("useAuth must be used within an AuthContext provider")
  }
  return value
}
