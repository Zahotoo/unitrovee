import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import {
    registerSchema,
    type RegisterFormValues,
} from '@/features/auth/registerSchema'

import { registerAccount } from '@/api/authApi'
import { useState } from 'react'
import { ApiError } from '@/api/client'
import { authPrimaryButtonClass } from '@/features/auth/authStyles'
import { AuthPageShell } from '@/features/auth/AuthPageShell'

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

export function RegisterPage() {
    const navigate = useNavigate()

    const [serverErrorMessage, setServerErrorMessage] = useState<string | null>(
        null,
    )

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            displayName: '',
            email: '',
            password: '',
        },
    })

    async function onSubmit(values: RegisterFormValues) {
        setServerErrorMessage(null)

        try {
            const response = await registerAccount(values)

            navigate(
                `/verify?email=${encodeURIComponent(response.data.email)}`,
            )
        } catch (error) {
            const errorCode = error instanceof ApiError ? getApiErrorCode(error.body) : undefined

            if (errorCode === 'EMAIL_ALREADY_EXISTS') {
                setServerErrorMessage(
                    'An account already exists for this email. Try logging in.',
                )
                return
            }
            if (errorCode === 'UNSUPPORTED_SCHOOL_EMAIL') {
                setServerErrorMessage(
                    'Use an email from a supported university.',
                )
                return
            }

            setServerErrorMessage(
                'Unable to create your account. Please try again.',
            )
        }
    }

    return (
        <AuthPageShell>
            <div className="relative mx-auto w-full max-w-md overflow-hidden rounded-2xl border bg-card p-6 shadow-sm sm:p-10">
                <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary to-secondary" />
                    <p className="mb-6 text-center text-2xl font-bold tracking-tight text-primary">
                        unitrovee
                    </p>
                    <div className="mb-8 space-y-2">
                        <h1 className="text-3xl font-semibold tracking-tight">
                            Create your account
                        </h1>
                        <p className="text-muted-foreground">
                            Use your university email and we’ll match it to your school automatically.
                        </p>
                    </div>
                {serverErrorMessage && (
                    <div
                        role="alert"
                        className="mb-5 rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive"
                    >
                        {serverErrorMessage}
                    </div>
                )}
                <form
                    className="space-y-5"
                    onSubmit={handleSubmit(onSubmit)}
                    noValidate
                >
                    <div className="space-y-2">
                        <label htmlFor="displayName" className="text-sm font-medium">
                            Display name
                        </label>
                        <input
                            id="displayName"
                            autoComplete="name"
                            aria-invalid={errors.displayName ? 'true' : undefined}
                            aria-describedby={
                                errors.displayName ? 'displayName-error' : undefined
                            }
                            className="h-11 w-full rounded-lg border-2 bg-muted px-3 text-foreground outline-none transition focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/20"
                            {...register('displayName')}
                        />
                        {errors.displayName && (
                            <p
                                id="displayName-error"
                                role="alert"
                                className="text-sm text-destructive"
                            >
                                {errors.displayName.message}
                            </p>
                        )}
                    </div>

                    <div className="space-y-2">
                        <label htmlFor="email" className="text-sm font-medium">
                            University email
                        </label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            aria-invalid={errors.email ? 'true' : undefined}
                            aria-describedby={errors.email ? 'email-error' : undefined}
                            className="h-11 w-full rounded-lg border-2 bg-muted px-3 text-foreground outline-none transition focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/20"
                            {...register('email')}
                        />
                        {errors.email && (
                            <p
                                id="email-error"
                                role="alert"
                                className="text-sm text-destructive"
                            >
                                {errors.email.message}
                            </p>
                        )}
                    </div>

                    <div className="space-y-2">
                        <label htmlFor="password" className="text-sm font-medium">
                            Password
                        </label>
                        <input
                            id="password"
                            type="password"
                            autoComplete="new-password"
                            aria-invalid={errors.password ? 'true' : undefined}
                            aria-describedby={errors.password ? 'password-error' : undefined}
                            className="h-11 w-full rounded-lg border-2 bg-muted px-3 text-foreground outline-none transition focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/20"
                            {...register('password')}
                        />
                        {errors.password && (
                            <p
                                id="password-error"
                                role="alert"
                                className="text-sm text-destructive"
                            >
                                {errors.password.message}
                            </p>
                        )}
                    </div>

                    <Button
                        type="submit"
                        variant="default"
                        className={`h-11 w-full ${authPrimaryButtonClass}`}
                        disabled={isSubmitting}
                    >
                        {isSubmitting ? 'Creating account…' : 'Create account'}
                    </Button>
                </form>

                <div className="relative my-7">
                    <div className="border-t"/>
                    <span className="absolute left-1/2 top-0 -translate-x-1/2 -translate-y-1/2 bg-card px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                        Or
                    </span>
                </div>

                <p className="mt-6 text-center text-sm text-muted-foreground">
                    Already have an account?{' '}
                    <Link to="/login" className="font-semibold text-primary underline-offset-2 transition hover:underline">
                        Log in
                    </Link>
                </p>
            </div>
        </AuthPageShell>
    )
}