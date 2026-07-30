import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

type PagePlaceholderProps = {
    title: string
    description: string
}

export function PagePlaceholder({title, description}: PagePlaceholderProps) {
    return (
        <section className="mx-auto flex w-full max-w-3xl flex-col items-center gap-5 px-6 py-20 text-center">
            <p className="text-sm font-medium tracking-wide text-muted-foreground uppercase">
                unitrovee
            </p>

            <div className="space-y-3">
                <h1 className="text-4xl font-semibold tracking-tight">{title}</h1>
                <p className="text-muted-foreground">{description}</p>
            </div>

            <Link
                to="/items"
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/80"
            >
                Browse items
                <ArrowRight aria-hidden="true" />
            </Link>
        </section>
    )
}