import { useQuery } from '@tanstack/react-query'

type SamplePost = {
    id: number
    title: string
}

async function fetchSamplePost(): Promise<SamplePost> {
    const response = await fetch(
        'https://jsonplaceholder.typicode.com/posts/1',
    )

    if (!response.ok) {
        throw new Error('Unable to load the sample post.')
    }
    return response.json()
}

export function QuerySample() {
    const { data, isError, isPending } = useQuery({
        queryKey: ['sample-post'],
        queryFn: fetchSamplePost,
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
                    This temporary page proves a query can load server data
                </p>
            </div>

            {isPending && <p role="status">Loading sample post...</p>}

            {isError && (
                <p role="alert" className="text-destructive">
                    Unable to load the sample post. Please try again.
                </p>
            )}

            {data && (
                <article className="w-full rounded-lg border bg-card p-6 text-left">
                    <p className="text-sm text-muted-foreground">Loaded post #{data.id}</p>
                    <h2 className="mt-2 text-xl font-semibold">{data.title}</h2>
                </article>
            )}
        </section>
    )
}