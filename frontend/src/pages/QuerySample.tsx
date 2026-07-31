import { useQuery } from '@tanstack/react-query'

import { apiClient } from '@/api/client'

type ApiResponse<T> = {
    data: T
    message: string
}

type PageResponse<T> = {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

type School = {
    id: number
    name: string
    shortName: string
    emailDomain: string
    city: string
}

async function fetchSchools(): Promise<School[]> {
    const response = await apiClient<ApiResponse<PageResponse<School>>>(
        '/schools?size=5',
    )

    return response.data.content
}

export function QuerySample() {
    const { data, isError, isPending } = useQuery({
        queryKey: ['schools', 'sample'],
        queryFn: fetchSchools,
        retry: false,
    })

    return (
        <section className="mx-auto flex w-full max-w-3xl flex-col items-center gap-5 px-6 py-20 text-center">
            <p className="text-sm font-medium tracking-wide text-muted-foreground uppercase">
                unitrovee
            </p>

            <div className="space-y-3">
                <h1 className="text-4xl font-semibold tracking-tight">
                    TanStack Query sample
                </h1>
                <p className="text-muted-foreground">
                    This page loads real school data from the Unitrovee API.
                </p>
            </div>

            {isPending && <p role="status">Loading schools...</p>}

            {isError && (
                <p role="alert" className="text-destructive">
                    Unable to load schools. Make sure the backend is running.
                </p>
            )}

            {data && (
                <article className="w-full rounded-lg border bg-card p-6 text-left">
                    <p className="text-sm text-muted-foreground">
                        Loaded {data.length} schools from the Unitrovee API
                    </p>

                    {data.length === 0 ? (
                        <p className="mt-3">No active schools were returned.</p>
                    ) : (
                        <ul className="mt-3 space-y-2">
                            {data.map((school) => (
                                <li key={school.id}>
                                    <span className="font-medium">{school.name}</span>
                                    <span className="text-muted-foreground">
                    {' '}
                                        — {school.city} · @{school.emailDomain}
                  </span>
                                </li>
                            ))}
                        </ul>
                    )}
                </article>
            )}
        </section>
    )
}