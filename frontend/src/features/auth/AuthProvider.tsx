import { useState } from 'react'
import type { PropsWithChildren } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import {
    apiClient,
    clearAccessToken,
    getAccessToken,
    setAccessToken,
} from '@/api/client'
import {
    AuthContext,
    type LoginCredentials,
} from '@/features/auth/auth-context'

type ApiResponse<T> = {
    data: T
    message: string
}

type LoginResponse = {
    accessToken: string
}

export function AuthProvider({ children }: PropsWithChildren) {
    const queryClient = useQueryClient()
    const [accessToken, setCurrentAccessToken] = useState<string | null>(() =>
        getAccessToken(),
    )

    async function login(credentials: LoginCredentials) {
        const response = await apiClient<ApiResponse<LoginResponse>>(
            '/auth/login',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(credentials),
                redirectOnUnauthorized: false,
            },
        )

        queryClient.clear()
        setAccessToken(response.data.accessToken)
        setCurrentAccessToken(response.data.accessToken)
    }

    function logout() {
        clearAccessToken()
        setCurrentAccessToken(null)
        queryClient.clear()
    }

    return (
        <AuthContext.Provider
            value={{
                accessToken,
                isAuthenticated: accessToken !== null,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}