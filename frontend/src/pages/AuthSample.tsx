import { useState } from "react"
import type { FormEvent } from "react"

import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/useAuth'

export function AuthSample() {
    const { accessToken, isAuthenticated, login, logout } = useAuth()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [isSubmitting, setIsSubmitting] = useState(false)

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage(null)
        setIsSubmitting(true)

        try {
            await login({ email, password })
            setPassword('')
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                setErrorMessage('Invalid email or password.')
            } else {
                setErrorMessage('Unable to log in. Please try again.')
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <section className="mx-auto flex w-full max-w-md flex-col gap-6 px-6 py-20">
            <div className="space-y-2">
                <p className="text-sm font-medium tracking-wide text-muted-foreground uppercase">
                    unitrovee
                </p>
                <h1 className="text-3xl font-semibold tracking-tight">
                    Auth state sample
                </h1>
                <p className="text-muted-foreground">
                    Temporary verification for Milestone 4.6.
                </p>
            </div>

            {isAuthenticated ? (
                <div className="space-y-4 rounded-lg border bg-card p-6">
                    <div>
                        <p className="font-medium">Authenticated</p>
                        <p className="mt-1 text-sm text-muted-foreground">
                            Token prefix: {accessToken?.slice(0, 16)}...
                        </p>
                    </div>

                    <Button type="button" variant="outline" onClick={logout}>
                        Log out
                    </Button>
                </div>
            ) : (
                <form className="space-y-4 rounded-lg border bg-card p-6" onSubmit={handleSubmit}>
                    <div className="space-y-2">
                        <label htmlFor="email" className="text-sm font-medium">
                            Email
                        </label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            className="h-11 w-full rounded-lg border bg-background px-3"
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <label htmlFor="password" className="text-sm font-medium">
                            Password
                        </label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            className="h-11 w-full rounded-lg border bg-background px-3"
                            required
                        />
                    </div>

                    {errorMessage && (
                        <p role="alert" className="text-sm text-destructive">
                            {errorMessage}
                        </p>
                    )}

                    <Button type="submit" className="w-full" disabled={isSubmitting}>
                        {isSubmitting ? 'Logging in...' : 'Log in'}
                    </Button>
                </form>
            )}
        </section>
    )
}