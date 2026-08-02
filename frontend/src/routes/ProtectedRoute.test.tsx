import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, test } from 'vitest'

import { AuthContext } from '@/features/auth/auth-context'
import type { AuthContextValue } from '@/features/auth/auth-context'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

const unauthenticatedAuth: AuthContextValue = {
    accessToken: null,
    isAuthenticated: false,
    login: async () => {},
    logout: () => {},
}

describe('ProtectedRoute', () => {
    test('redirects an unauthenticated visitor to the login page', () => {
        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <MemoryRouter initialEntries={['/profile']}>
                    <Routes>
                        <Route element={<ProtectedRoute />}>
                            <Route path="/profile" element={<h1>Protected page</h1>} />
                        </Route>
                        <Route path="/login" element={<h1>Log in</h1>} />
                    </Routes>
                </MemoryRouter>
            </AuthContext.Provider>,
        )

        expect(
            screen.getByRole('heading', { name: 'Log in' }),
        ).toBeInTheDocument()

        expect(
            screen.queryByRole('heading', { name: 'Protected page' }),
        ).not.toBeInTheDocument()
    })
})