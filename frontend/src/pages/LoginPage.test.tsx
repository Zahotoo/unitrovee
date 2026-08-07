import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { RouterProvider } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { ApiError } from '@/api/client'

import {
    AuthContext,
    type AuthContextValue,
} from '@/features/auth/auth-context'
import { router } from '@/routes/router'

const unauthenticatedAuth: AuthContextValue = {
    accessToken: null,
    isAuthenticated: false,
    login: async () => {},
    logout: () => {}
}

afterEach(() => {
    cleanup()
})

describe('LoginPage', () => {
    test('shows an accessible sign-in form at /login', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        expect(
            screen.getByRole('heading', { name: 'Welcome back' }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('textbox', { name: 'School email' }),
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText('Password'),
        ).toHaveAttribute('type', 'password')

        expect(
            screen.getByRole('button', { name: 'Sign in' }),
        ).toBeInTheDocument()

        expect(
            screen.getByRole('link', { name: 'Create an account' }),
        ).toHaveAttribute('href', '/register')

        expect(
            screen.queryByText('Remember me'),
        ).not.toBeInTheDocument()

        expect(
            screen.queryByText('Forgot password?'),
        ).not.toBeInTheDocument()
    })

    test('shows a required error when school email is empty', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        expect(
            await screen.findByText('Email is required'),
        ).toBeInTheDocument()
    })

    test('passes valid credentials to the auth context', async () => {
        const login = vi.fn().mockResolvedValue(undefined)

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    ...unauthenticatedAuth,
                    login,
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        await waitFor(() => {
            expect(login).toHaveBeenCalledWith({
                email: 'aoife@ucdconnect.ie',
                password: 'password8',
            })
        })
    })

    test('returns the user to their original protected page after login', async () => {
        const login = vi.fn().mockResolvedValue(undefined)

        await router.navigate('/login', {
            state: {
                from: {
                    pathname: '/profile',
                    search: '',
                    hash: '',
                },
            },
        })

        render(
            <AuthContext.Provider
                value={{
                    accessToken: 'test-token',
                    isAuthenticated: true,
                    login,
                    logout: () => {},
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        await waitFor(() => {
            expect(router.state.location.pathname).toBe('/profile')
        })
    })

    test('explains when the email or password is incorrect', async () => {
        const login = vi.fn().mockRejectedValue(
            new ApiError(401, {
                error: {
                    code: 'INVALID_CREDENTIALS',
                    message: 'Invalid credentials',
                },
            }),
        )

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    ...unauthenticatedAuth,
                    login,
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'wrongpass8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'Email or password is incorrect.',
        )

        expect(router.state.location.pathname).toBe('/login')
    })

    test('disables login controls while sign-in is pending', async () => {
        const login = vi.fn().mockReturnValue(
            new Promise<void>(() => {}),
        )

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    ...unauthenticatedAuth,
                    login,
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        const emailInput = screen.getByRole('textbox', {
            name: 'School email',
        })
        const passwordInput = screen.getByLabelText('Password')
        const submitButton = screen.getByRole('button', {
            name: 'Sign in',
        })

        fireEvent.change(emailInput, {
            target: { value: 'aoife@ucdconnect.ie' },
        })
        fireEvent.change(passwordInput, {
            target: { value: 'password8' },
        })
        fireEvent.click(submitButton)

        await waitFor(() => {
            expect(submitButton).toBeDisabled()
        })

        expect(emailInput).toBeDisabled()
        expect(passwordInput).toBeDisabled()
        expect(submitButton).toHaveTextContent('Signing in...')
    })

    test('shows a generic message when sign-in cannot be completed', async () => {
        const login = vi.fn().mockRejectedValue(
            new ApiError(500, {
                error: {
                    code: 'INTERNAL_ERROR',
                    message: 'An unexpected error occurred.',
                },
            }),
        )

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    ...unauthenticatedAuth,
                    login,
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'Unable to sign in. Please try again.',
        )
    })

    test('blocks invalid email and short password before calling auth', async () => {
        const login = vi.fn()

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    ...unauthenticatedAuth,
                    login,
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'not-an-email' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'short' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        expect(
            await screen.findByText('Email must be valid'),
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Password must be between 8 and 72 characters',
            ),
        ).toBeInTheDocument()

        expect(login).not.toHaveBeenCalled()
    })

    test('navigates home after login when there is no original destination', async () => {
        const login = vi.fn().mockResolvedValue(undefined)

        await router.navigate('/login')

        render(
            <AuthContext.Provider
                value={{
                    accessToken: 'test-token',
                    isAuthenticated: true,
                    login,
                    logout: () => {},
                }}
            >
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        fireEvent.change(
            screen.getByRole('textbox', { name: 'School email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Sign in' }),
        )

        await waitFor(() => {
            expect(router.state.location.pathname).toBe('/')
        })
    })

    test('shows the shared gradient accent on the login card', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        expect(
            screen.getByTestId('auth-card-gradient'),
        ).toHaveClass(
            'bg-gradient-to-r',
            'from-primary',
            'to-secondary',
        )
    })

    test('shows a masked hint in the blue password field', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        const passwordInput = screen.getByLabelText('Password')

        expect(passwordInput).toHaveAttribute(
            'placeholder',
            '••••••••',
        )
        expect(passwordInput).toHaveClass('bg-muted')
        expect(passwordInput).toHaveClass(
            'placeholder:text-muted-foreground/60',
        )
    })

    test('shows an underline when Create an account is hovered', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        const registerLink = screen.getByRole('link', {
            name: 'Create an account',
        })

        expect(registerLink).not.toHaveClass('underline')
        expect(registerLink).not.toHaveClass(
            'decoration-secondary/40',
        )
        expect(registerLink).toHaveClass('font-semibold')
        expect(registerLink).toHaveClass('hover:underline')
    })

    test('uses the shared auth header hierarchy', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        const main = within(screen.getByRole('main'))

        expect(main.getByText('unitrovee')).toHaveClass(
            'text-primary',
        )

        expect(
            main.getByRole('heading', { name: 'Welcome back' }),
        ).not.toHaveClass('text-primary')

        expect(
            main.getByText(
                'Security access your student marketplace.',
            ),
        ).toHaveClass('text-muted-foreground')
    })

    test('uses the same neutral card elevation as the other auth pages', async () => {
        await router.navigate('/login')

        render(
            <AuthContext.Provider value={unauthenticatedAuth}>
                <RouterProvider router={router} />
            </AuthContext.Provider>,
        )

        const gradientBar = screen.getByTestId('auth-card-gradient')
        const card = gradientBar.parentElement

        expect(card).not.toBeNull()
        expect(card).toHaveClass('shadow-sm')
        expect(card).not.toHaveClass(
            'shadow-[6px_6px_0px_0px_rgba(242,153,74,0.18)]',
        )
    })
})