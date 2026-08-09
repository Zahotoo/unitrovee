import type { ReactNode } from 'react'

type AuthPageShellProps = {
    children: ReactNode
}

export function AuthPageShell({ children }: AuthPageShellProps) {
    return (
        <section
            data-testid="auth-page-background"
            className="relative isolate flex flex-1 overflow-hidden bg-background px-4 py-12 sm:px-6 md:py-20"
        >
            <div
                data-testid="auth-page-blue-blob"
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_65%_55%_at_0%_0%,var(--primary),transparent_72%)] opacity-[0.6]"
            />
            <div
                data-testid="auth-page-orange-blob"
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_60%_60%_at_100%_100%,var(--secondary),transparent_72%)] opacity-[0.6]"
            />

            <div
                data-testid="auth-page-content"
                className="relative z-10 flex w-full flex-1 items-center"
            >
                {children}
            </div>
        </section>
    )
}