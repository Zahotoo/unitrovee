import { Link, NavLink } from 'react-router-dom'
import { Button } from '@/components/ui/button'

import { cn } from '@/lib/utils'

const navigation = [
    { to: '/items', label: 'Browse' },
    { to: '/items/new', label: 'Post an item' },
]

export function Header() {
    return (
        <header className="border-b">
            <div className="mx-auto flex min-h-16 max-w-6xl items-center justify-between gap-4 px-6">
                <Link to="/" className="text-lg font-semibold tracking-tight">
                    unitrovee
                </Link>

                <nav className="flex items-center gap-1" aria-label="Main navigation">
                    {navigation.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            className={({ isActive }) =>
                                cn(
                                    'rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground',
                                    isActive && 'bg-muted text-foreground',
                                )
                            }
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <div className="flex items-center gap-2">
                    <Link
                        to="/login"
                        className="rounded-md px-3 py-2 text-sm font-medium hover:bg-muted"
                    >
                        Log in
                    </Link>
                    <Button
                        render={<Link to="/register" />}
                        nativeButton={false}
                        className="h-11 px-4"
                    >
                        Register
                    </Button>
                </div>
            </div>
        </header>
    )
}