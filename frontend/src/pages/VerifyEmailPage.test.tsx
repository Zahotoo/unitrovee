import {
    cleanup,
    fireEvent,
    render,
    screen,
    waitFor,
} from '@testing-library/react'
import { RouterProvider } from 'react-router-dom'
import {
    afterEach,
    beforeEach,
    describe,
    expect,
    test,
    vi,
} from 'vitest'

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

const successfulVerificationResponse = {
    data: {
        email: 'aoife@ucdconnect.ie',
        emailVerified: true,
    },
    message: 'Success',
}

beforeEach(() => {
    mockedApiClient.mockResolvedValue(successfulVerificationResponse)
})

afterEach(() => {
    cleanup()
    vi.clearAllMocks()
})

describe('VerifyEmailPage', () => {
    test('shows the six-digit verification form for the email in the URL', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        expect(
            screen.getByRole('heading', { name: 'Check your inbox' }),
        ).toBeInTheDocument()

        expect(
            screen.getByText('aoife@ucdconnect.ie'),
        ).toBeInTheDocument()

        expect(
            screen.getAllByRole('textbox'),
        ).toHaveLength(6)

        expect(
            screen.getByRole('button', { name: 'Verify Email' }),
        ).toBeInTheDocument()
    })

    test('moves focus to the next digit after a number is entered', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        fireEvent.change(digitInputs[0], {
            target: { value: '4' },
        })

        expect(digitInputs[0]).toHaveValue('4')
        expect(digitInputs[1]).toHaveFocus()
    })

    test('moves focus back when Backspace is pressed in an empty digit input', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        fireEvent.change(digitInputs[0], {
            target: { value: '4' },
        })

        fireEvent.keyDown(digitInputs[1], {
            key: 'Backspace',
        })

        expect(digitInputs[0]).toHaveFocus()
    })

    test('fills all six inputs when a full code is pasted', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        fireEvent.paste(digitInputs[0], {
            clipboardData: {
                getData: () => '123456',
            },
        })

        expect(digitInputs[0]).toHaveValue('1')
        expect(digitInputs[1]).toHaveValue('2')
        expect(digitInputs[2]).toHaveValue('3')
        expect(digitInputs[3]).toHaveValue('4')
        expect(digitInputs[4]).toHaveValue('5')
        expect(digitInputs[5]).toHaveValue('6')
        expect(digitInputs[5]).toHaveFocus()
    })

    test('ignores a non-digit entered into an OTP input', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        fireEvent.change(digitInputs[0], {
            target: { value: 'a' },
        })

        expect(digitInputs[0]).toHaveValue('')
    })

    test('sends the email and six-digit code to the verification endpoint', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        for (const [index, digit] of [...'123456'].entries()) {
            fireEvent.change(digitInputs[index], {
                target: { value: digit },
            })
        }

        fireEvent.click(
            screen.getByRole('button', { name: 'Verify Email' }),
        )

        await waitFor(() => {
            expect(mockedApiClient).toHaveBeenCalledWith('/auth/verify', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email: 'aoife@ucdconnect.ie',
                    code: '123456',
                }),
            })
        })
    })

    test('navigates to login after successful verification', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        for (const [index, digit] of [...'123456'].entries()) {
            fireEvent.change(digitInputs[index], {
                target: { value: digit },
            })
        }

        fireEvent.click(
            screen.getByRole('button', { name: 'Verify Email' }),
        )

        await waitFor(() => {
            expect(router.state.location.pathname).toBe('/login')
        })
    })

    test('explains when the verification code is invalid or expired', async () => {
        mockedApiClient.mockRejectedValueOnce(
            new ApiError(400, {
                error: {
                    code: 'INVALID_VERIFICATION_CODE',
                    message: 'Verification code is invalid or expired',
                },
            }),
        )

        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        for (const [index, digit] of [...'123456'].entries()) {
            fireEvent.change(digitInputs[index], {
                target: { value: digit },
            })
        }

        fireEvent.click(
            screen.getByRole('button', { name: 'Verify Email' }),
        )

        expect(
            await screen.findByRole('alert'),
        ).toHaveTextContent(
            'That verification code is invalid or has expired. Please try again.',
        )

        expect(router.state.location.pathname).toBe('/verify')
    })

    test('requires all six digits before submitting', async () => {
        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        fireEvent.change(digitInputs[0], {
            target: { value: '1' },
        })

        fireEvent.click(
            screen.getByRole('button', { name: 'Verify Email' }),
        )

        expect(
            await screen.findByText('Enter all 6 digits.'),
        ).toBeInTheDocument()

        expect(mockedApiClient).not.toHaveBeenCalled()
    })

    test('guides the user back to registration when the email is missing', async () => {
        await router.navigate('/verify')

        render(<RouterProvider router={router} />)

        expect(
            screen.getByRole('alert'),
        ).toHaveTextContent(
            'Your verification link is incomplete. Please create your account again.',
        )

        expect(
            screen.getByRole('link', { name: 'Create an account' }),
        ).toHaveAttribute('href', '/register')

        expect(mockedApiClient).not.toHaveBeenCalled()
    })

    test('disables verification while the request is pending', async () => {
        mockedApiClient.mockReturnValueOnce(new Promise(() => {}))

        await router.navigate('/verify?email=aoife%40ucdconnect.ie')

        render(<RouterProvider router={router} />)

        const digitInputs = screen.getAllByRole('textbox')

        for (const [index, digit] of [...'123456'].entries()) {
            fireEvent.change(digitInputs[index], {
                target: { value: digit },
            })
        }

        const submitButton = screen.getByRole('button', {
            name: 'Verify Email',
        })

        fireEvent.click(submitButton)

        await waitFor(() => {
            expect(submitButton).toBeDisabled()
        })

        for (const digitInput of digitInputs) {
            expect(digitInput).toBeDisabled()
        }

        expect(submitButton).toHaveTextContent('Verifying…')
    })
})