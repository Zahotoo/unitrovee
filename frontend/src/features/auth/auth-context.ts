import { createContext } from "react"

export type LoginCredentials = {
    email: string
    password: string
}

export type AuthContextValue = {
    accessToken: string | null
    isAuthenticated: boolean
    login: (credentials: LoginCredentials) => Promise<void>
    logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)