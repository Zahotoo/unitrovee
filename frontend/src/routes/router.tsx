import { createBrowserRouter } from 'react-router-dom'

import App from '@/app/App'
import { PagePlaceholder } from '@/pages/PagePlaceholder'
import { QuerySample } from '@/pages/QuerySample'
import { AuthSample } from '@/pages/AuthSample'

const placeholder = (title: string, description: string) => (
    <PagePlaceholder title={title} description={description} />
)

export const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        children: [
            {
                index: true,
                element: placeholder(
                    'Find more. Waste less.',
                    'Buy, sell, swap, or give away items with verified students.',
                ),
            },
            {
                path: 'query-sample',
                element: <QuerySample />,
            },
            {
                path: 'auth-sample',
                element: <AuthSample />,
            },
            {
                path: 'login',
                element: placeholder('Log in', 'Access your unitrovee account.'),
            },
            {
                path: 'register',
                element: placeholder(
                    'Create an account', 'Join the student marketplace for your university.',
                ),
            },
            {
                path: 'items',
                element: placeholder('Browse items', 'Discover listings from students.'),
            },
            {
                path: 'items/:id',
                element: placeholder('Item details', 'View a listing and its details.'),
            },
            {
                path: 'items/new',
                element: placeholder('Post an item', 'Create a new listing.'),
            },
            {
                path: 'profile',
                element: placeholder('Profile', 'View and manage your account.'),
            },
            {
                path: 'my-listings',
                element: placeholder(
                    'My listings',
                    'Manage items you have posted on unitrovee.',
                ),
            },
            {
                path: 'favorites',
                element: placeholder('Favorites', 'View saved listings.'),
            },
            {
                path: 'admin',
                element: placeholder('Admin', 'Manage the platform.'),
            },
            {
                path: 'support',
                element: placeholder('Support', 'Get help with unitrovee.'),
            },
            {
                path: '*',
                element: placeholder('Page not found', 'The page you requested does not exist.'),
            },
        ],
    },
])
