import { ArrowLeft, ArrowRight, Mail } from 'lucide-react'
import { type FormEvent, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resendVerificationCode, verifyEmail } from '@/api/authApi'
import { ApiError } from '@/api/client'
import { verifyEmailSchema } from '@/features/auth/verifyEmailSchema'
import { authPrimaryButtonClass } from '@/features/auth/authStyles'

type ErrorResponseBody = {
    error?: {
        code?: string
    }
}

function getApiErrorCode(body: unknown): string | undefined {
    if (typeof body !== 'object' || body === null || !('error' in body)) {
        return undefined
    }
    const errorBody = body as ErrorResponseBody
    return errorBody.error?.code
}

export function VerifyEmailPage() {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const email = searchParams.get('email') ?? ''

    const [digits, setDigits] = useState<string[]>(
        Array(6).fill(''),
    )
    const [serverErrorMessage, setServerErrorMessage] = useState<string | null>(
        null,
    )
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [isResending, setIsResending] = useState(false)
    const [resendMessage, setResendMessage] = useState<string | null>(null)
    const [resendCooldownSeconds, setResendCooldownSeconds] = useState(0)
    const inputRefs = useRef<Array<HTMLInputElement | null>>([])

    function handleDigitChange(index: number, value: string) {
        const digit = value.replace(/\D/g, '').slice(-1)

        setDigits((currentDigits) =>
            currentDigits.map((currentDigit, digitIndex) =>
                digitIndex === index ? digit : currentDigit,
            ),
        )

        if (digit && index < 5) {
            inputRefs.current[index + 1]?.focus()
        }
    }

    function handleDigitKeyDown(index: number, key: string) {
        if (key === 'Backspace' && digits[index] === '' && index > 0) {
            inputRefs.current[index - 1]?.focus()
        }
    }

    function handlePaste(pastedText: string) {
        const pastedDigits = pastedText
            .replace(/\D/g, '')
            .slice(0, 6)

        if (pastedDigits.length === 0) {
            return
        }

        const nextDigits = Array.from(
            { length: 6 },
            (_, index) => pastedDigits[index] ?? '',
        )

        setDigits(nextDigits)

        const lastFilledIndex = pastedDigits.length - 1
        inputRefs.current[lastFilledIndex]?.focus()
    }

    useEffect(() => {
        if (resendCooldownSeconds === 0) {
            return
        }

        const timeoutId = window.setTimeout(() => {
            setResendCooldownSeconds((seconds) => seconds - 1)
        }, 1_000)

        return () => window.clearTimeout(timeoutId)
    }, [resendCooldownSeconds])

    async function handleResend() {
        setResendMessage(null)
        setIsResending(true)

        try {
            const response = await resendVerificationCode({ email })

            setResendCooldownSeconds(response.data.retryAfterSeconds)
            setResendMessage('Check your inbox - your code may already be there.')
        } catch {
            setResendMessage('Unable to resend the code. Please try again.')
        } finally {
            setIsResending(false)
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setServerErrorMessage(null)

        const validationResult = verifyEmailSchema.safeParse({
            email,
            code: digits.join(''),
        })

        if (!validationResult.success) {
            setServerErrorMessage(
                validationResult.error.issues[0].message,
            )
            return
        }

        setIsSubmitting(true)

        try {
            await verifyEmail(validationResult.data)
            navigate('/login')
        } catch (error) {
            const errorCode =
                error instanceof ApiError
                    ? getApiErrorCode(error.body)
                    : undefined

            if (errorCode === 'INVALID_VERIFICATION_CODE') {
                setServerErrorMessage(
                    'That verification code is invalid or has expired. Please try again.',
                )
                return
            }
            setServerErrorMessage(
                'Unable to verify your email. Please try again.',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    if (!email) {
        return (
            <section className="bg-background px-4 py-12 sm:px-6 md:py-20">
                <div className="relative mx-auto w-full max-w-md overflow-hidden rounded-2xl border bg-card p-6 text-center shadow-sm sm:p-10">
                    <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary to-secondary" />

                    <h1 className="text-2xl font-semibold tracking-tight">
                        Verification link is incomplete
                    </h1>

                    <p
                        role="alert"
                        className="mt-3 text-muted-foreground"
                    >
                        Your verification link is incomplete. Please create your account again.
                    </p>

                    <Link
                        to="/register"
                        className={`mt-6 inline-flex h-11 items-center justify-center rounded-lg px-5 font-medium ${authPrimaryButtonClass}`}
                    >
                        Create an account
                    </Link>
                </div>
            </section>
        )
    }

    return (
        <section className="bg-background px-4 py-12 sm:px-6 md:py-20">
            <div className="relative mx-auto w-full max-w-md overflow-hidden rounded-2xl border bg-card p-6 shadow-sm sm:p-10">
                <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary to-secondary" />

                <p className="mb-6 text-center text-2xl font-bold tracking-tight text-primary">
                    unitrovee
                </p>

                <div className="relative mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <Mail className="size-8" aria-hidden="true" />
                    <span className="absolute -right-1 -top-1 size-4 rounded-full border-2 border-card bg-secondary" />
                </div>

                <div className="mb-8 text-center">
                    <h1 className="text-3xl font-semibold tracking-tight">
                        Check your inbox
                    </h1>
                    <p className="mt-2 text-muted-foreground">
                        We&apos;ve sent a 6-digit verification code to{' '}
                        <strong className="font-semibold text-foreground">
                            {email}
                        </strong>
                        . Enter it below to start swapping.
                    </p>
                </div>
                {serverErrorMessage && (
                    <div
                        role="alert"
                        className="mb-6 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-center text-sm text-destructive"
                    >
                        {serverErrorMessage}
                    </div>
                )}
                <form onSubmit={handleSubmit}>
                    <div
                        className="flex justify-between gap-2 sm:gap-3"
                        aria-label="Six digit verification code"
                    >
                        {Array.from({ length: 6 }, (_, index) => (
                            <input
                                key={index}
                                ref={(element) => {
                                    inputRefs.current[index] = element
                                }}
                                aria-label={`Verification code digit ${index + 1}`}
                                autoFocus={index === 0}
                                disabled={isSubmitting}
                                className="h-14 w-full rounded-lg border bg-muted text-center text-2xl font-semibold outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                                inputMode="numeric"
                                maxLength={1}
                                type="text"
                                value={digits[index]}
                                onChange={(event) =>
                                    handleDigitChange(index, event.target.value)
                                }
                                onKeyDown={(event) =>
                                    handleDigitKeyDown(index, event.key)
                                }
                                onPaste={(event) => {
                                    event.preventDefault()
                                    handlePaste(event.clipboardData.getData('text'))
                                }}
                            />
                        ))}
                    </div>

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className={`mt-6 flex h-12 w-full items-center justify-center gap-2 rounded-lg font-medium ${authPrimaryButtonClass}`}
                    >
                        {isSubmitting ? (
                            'Verifying…'
                        ) : (
                            <>
                                Verify Email
                                <ArrowRight className="size-5" aria-hidden="true" />
                            </>
                        )}
                    </button>
                </form>

                <div className="mt-6 border-t pt-6 text-center text-sm text-muted-foreground">
                    <div className="flex flex-wrap items-center justify-center gap-x-1.5 gap-y-1">
                        <span>Didn&apos;t receive the code?</span>

                        <button
                            type="button"
                            disabled={isResending || resendCooldownSeconds > 0}
                            onClick={handleResend}
                            className="font-semibold text-primary transition-colors hover:text-primary/80 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {isResending
                                ? 'Resending...'
                                : resendCooldownSeconds > 0
                                    ? `Resend code in ${resendCooldownSeconds}s`
                                    : 'Resend'
                            }
                        </button>
                    </div>
                    {resendMessage && (
                        <p
                            role="status"
                            className="mt-3 text-center text-sm text-muted-foreground"
                        >
                            {resendMessage}
                        </p>
                    )}
                    <Link
                        to="/login"
                        className="mt-4 flex items-center justify-center gap-1 text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground"
                    >
                        <ArrowLeft className="size-4" aria-hidden="true" />
                        Back to Login
                    </Link>
                </div>
            </div>
        </section>
    )
}