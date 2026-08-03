import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { RouterProvider } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import { ApiError, apiClient } from '@/api/client'
import { router } from '@/routes/router'

vi.mock('@/api/client', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/api/client')>()

    return {
        ...actual,
        apiClient: vi.fn(),
    }
})

const mockedApiClient = vi.mocked(apiClient)

const successfulRegistrationResponse = {
    data: {
        id: 1,
        email: 'aoife@ucdconnect.ie',
        displayName: 'Aoife Murphy',
        emailVerified: false,
        school: {
            id: 1,
            name: 'University College Dublin',
            shortName: 'UCD',
            emailDomain: 'ucdconnect.ie',
            city: 'Dublin',
        },
    },
    message: 'Success',
}

beforeEach(() => {
    mockedApiClient.mockResolvedValue(successfulRegistrationResponse)
})

afterEach(() => {
    cleanup()
    vi.clearAllMocks()
})

describe('RegisterPage', () => {
    test('shows an accessible account-creation form at /register', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        expect(
            screen.getByRole('textbox', { name: 'Display name' }),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('textbox', { name: 'University email' }),
        ).toBeInTheDocument()
        expect(
            screen.getByLabelText('Password'),
        ).toHaveAttribute('type', 'password')
        expect(
            screen.getByRole('button', { name: 'Create account' }),
        ).toBeInTheDocument()
    })

    test('shows a required error when display name is empty', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText('Display name is required')
        ).toBeInTheDocument()
    })

    test('shows a required error when university email is empty', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText('Email is required'),
        ).toBeInTheDocument()
    })

    test('shows a format error for an invalid university email', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'not-an-email' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText('Email must be valid'),
        ).toBeInTheDocument()
    })

    test('shows a required error when password is empty', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText('Password is required')
        ).toBeInTheDocument()
    })

    test('shows an error when password is shorter than 8 characters', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'short' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'Password must be between 8 and 72 characters',
            ),
        ).toBeInTheDocument()
    })

    test('shows an error when password is longer than 72 characters', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'a'.repeat(73) } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'Password must be between 8 and 72 characters',
            ),
        ).toBeInTheDocument()
    })

    test('sends valid registration details to the backend', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        await waitFor(() => {
            expect(mockedApiClient).toHaveBeenCalledWith('/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    displayName: 'Aoife Murphy',
                    email: 'aoife@ucdconnect.ie',
                    password: 'password8',
                }),
            })
        })
    })

    test('navigates to verification with the normalized backend email', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'Aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        await waitFor(() => {
            expect(router.state.location.pathname).toBe('/verify')
        })

        expect(router.state.location.search).toBe(
            '?email=aoife%40ucdconnect.ie',
        )
    })

    test('explains how to recover when the email is already registered', async () => {
        mockedApiClient.mockRejectedValueOnce(
            new ApiError(409, {
                error: {
                    code: 'EMAIL_ALREADY_EXISTS',
                    message: 'Email is already registered',
                },
            }),
        )

        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'An account already exists for this email. Try logging in.',
            ),
        ).toBeInTheDocument()
    })

    test('explains that registration requires a supported university email', async () => {
        mockedApiClient.mockRejectedValueOnce(
            new ApiError(400, {
                error: {
                    code: 'UNSUPPORTED_SCHOOL_EMAIL',
                    message: 'Email domain is not supported by unitrovee',
                },
            }),
        )

        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@gmail.com' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'Use an email from a supported university.',
            ),
        ).toBeInTheDocument()
    })

    test('disables submission while registration is pending', async () => {
        mockedApiClient.mockReturnValueOnce(new Promise(() => {}))

        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        const submitButton = screen.getByRole('button', {
            name: 'Create account',
        })

        fireEvent.click(submitButton)

        await waitFor(() => {
            expect(submitButton).toBeDisabled()
        })

        expect(submitButton).toHaveTextContent('Creating account…')
    })

    test('shows an error when display name is longer than 100 characters', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'a'.repeat(101) } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'Display name must be at most 100 characters',
            ),
        ).toBeInTheDocument()
    })

    test('shows an error when email is longer than 254 characters', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        const longEmail = `${'a'.repeat(243)}@ucdconnect.ie`

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: longEmail } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: 'password8' } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        expect(
            await screen.findByText(
                'Email must be at most 254 characters',
            ),
        ).toBeInTheDocument()
    })

    test('preserves password whitespace when submitting registration', async () => {
        await router.navigate('/register')

        render(<RouterProvider router={router} />)

        const password = ' password8 '

        fireEvent.change(
            screen.getByRole('textbox', { name: 'Display name' }),
            { target: { value: 'Aoife Murphy' } },
        )
        fireEvent.change(
            screen.getByRole('textbox', { name: 'University email' }),
            { target: { value: 'aoife@ucdconnect.ie' } },
        )
        fireEvent.change(
            screen.getByLabelText('Password'),
            { target: { value: password } },
        )

        fireEvent.click(
            screen.getByRole('button', { name: 'Create account' }),
        )

        await waitFor(() => {
            expect(mockedApiClient).toHaveBeenCalledWith(
                '/auth/register',
                expect.objectContaining({
                    body: JSON.stringify({
                        displayName: 'Aoife Murphy',
                        email: 'aoife@ucdconnect.ie',
                        password,
                    }),
                }),
            )
        })
    })
})
