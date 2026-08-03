import { Link } from 'react-router-dom'

export function Footer() {
    return (
        <footer className="border-t bg-sidebar">
            <div className="mx-auto flex max-w-7xl flex-col gap-3 px-6 py-6 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
                <p>&copy; 2026 unitrovee. Built for Irish students.</p>

                <nav className="flex gap-4" aria-label="Footer navigation">
                    <Link to="/support" className="hover:text-foreground">
                        Support
                    </Link>
                    <Link to="/admin" className="hover:text-foreground">
                        Admin
                    </Link>
                </nav>
            </div>
        </footer>
    )
}