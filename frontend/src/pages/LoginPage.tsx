import { zodResolver } from '@hookform/resolvers/zod'
import { LockKeyhole, Mail } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useState } from 'react'
import { ApiError } from '@/api/client'
import { authPrimaryButtonClass } from '@/features/auth/authStyles'

import {
    loginSchema,
    type LoginFormValues,
} from '@/features/auth/loginSchema'

type LoginLocationState = {
    from?: {
        pathname: string
        search?: string
        hash?: string
    }
}

export function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const [serverErrorMessage, setServerErrorMessage] = useState<string | null>(null)
    const { login } = useAuth()

    async function onSubmit(values: LoginFormValues) {
        setServerErrorMessage(null)

        try {
            await login(values)

            const state = location.state as LoginLocationState | null
            const from = state?.from

            const destination = from
                ? `${from.pathname}${from.search ?? ''}${from.hash ?? ''}`
                : '/'

            navigate(destination, { replace: true })
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                setServerErrorMessage(
                    'Email or password is incorrect.',
                )
                return
            }

            setServerErrorMessage(
                'Unable to sign in. Please try again.',
            )
        }

    }

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            email: '',
            password: '',
        },
    })

    return (
        <section className="bg-background px-4 py-12 sm:px-6 md:py-20">
            <div className="relative mx-auto w-full max-w-md overflow-hidden rounded-2xl border bg-card p-6 shadow-sm sm:p-10">
                <div
                    data-testid="auth-card-gradient"
                    aria-hidden="true"
                    className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary to-secondary"
                />

                <p className="mb-6 text-center text-2xl font-bold tracking-tight text-primary">
                    unitrovee
                </p>

                <div className="mb-8 space-y-2">
                    <h1 className="text-3xl font-semibold tracking-tight">
                        Welcome back
                    </h1>
                    <p className="text-muted-foreground">
                        Security access your student marketplace.
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
                    noValidate
                    onSubmit={handleSubmit(onSubmit)}
                >
                    <div className="space-y-2">
                        <label
                            htmlFor="email"
                            className="text-sm font-medium"
                        >
                            School email
                        </label>

                        <div className="relative">
                            <Mail
                                className="pointer-events-none absolute left-3 top-1/2 size-5 -translate-y-1/2 text-muted-foreground"
                                aria-hidden="true"
                            />

                            <input
                                id="email"
                                type="email"
                                autoComplete="email"
                                placeholder="yourname@school.ie"
                                aria-invalid={
                                    errors.email ? 'true' : undefined
                                }
                                aria-describedby={
                                    errors.email
                                        ? 'email-error'
                                        : undefined
                                }
                                disabled={isSubmitting}
                                className="h-12 w-full rounded-xl border-2 bg-muted py-2 pl-10 pr-3 text-foreground outline-none transition focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/20"
                                {...register('email')}
                            />
                        </div>

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
                        <label
                            htmlFor="password"
                            className="text-sm font-medium"
                        >
                            Password
                        </label>

                        <div className="relative">
                            <LockKeyhole
                                className="pointer-events-none absolute left-3 top-1/2 size-5 -translate-y-1/2 text-muted-foreground"
                                aria-hidden="true"
                            />

                            <input
                                id="password"
                                type="password"
                                autoComplete="current-password"
                                placeholder="••••••••"
                                aria-invalid={
                                    errors.password ? 'true' : undefined
                                }
                                aria-describedby={
                                    errors.password
                                        ? 'password-error'
                                        : undefined
                                }
                                disabled={isSubmitting}
                                className="h-12 w-full rounded-xl border-2 bg-muted py-2 pl-10 pr-3 text-foreground placeholder:text-muted-foreground/60 outline-none transition focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/20"
                                {...register('password')}
                            />
                        </div>

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

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className={`mt-2 flex h-12 w-full items-center justify-center rounded-xl font-semibold ${authPrimaryButtonClass}`}
                    >
                        {isSubmitting ? 'Signing in...' : 'Sign in'}
                    </button>
                </form>

                <div className="relative my-7">
                    <div className="border-t"/>
                    <span className="absolute left-1/2 top-0 -translate-x-1/2 -translate-y-1/2 bg-card px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                        Or
                    </span>
                </div>

                <div className="p-1/2 text-center text-sm text-muted-foreground">
                    New to unitrovee?{' '}
                    <Link
                        to="/register"
                        className="font-semibold text-primary underline-offset-2 transition hover:underline"
                    >
                        Create an account
                    </Link>
                </div>
            </div>
        </section>
    )
}